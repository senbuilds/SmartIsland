/*
 * Smart Island · Galaxy Z Fold8 edition (2026)
 * © Sen Hu — github.com/senbuilds
 * Based on Smart Island by Animesh Gupta (github.com/agupta07505)
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/** Where every island surface should rest. All x values are dp offsets from the window center. */
data class IslandMotionTargets(
    val mainX: Float,
    val mainY: Float,
    val mainW: Float,
    val mainH: Float,
    val mainR: Float,
    val pillW: Float,
    val pillH: Float,
    val hasSecondary: Boolean,
    val secX: Float,
    val secW: Float,
    val secR: Float,
    val circle: Float,
)

/**
 * Choreographs the island as one liquid: the main surface swells and flows open, the
 * companion droplet is drawn into the vacated spot, and on collapse the main droplet
 * squeezes the companion out ("pop") before landing with a weighted "thud".
 *
 * Only outlines deform. Content layers read [expandedAlpha] / [collapsedAlpha] and are
 * never stretched.
 */
@Stable
class IslandMotion(initial: IslandMotionTargets) {
    // Main surface
    val mainX = Animatable(initial.mainX)
    val mainY = Animatable(initial.mainY)
    val mainW = Animatable(initial.mainW)
    val mainH = Animatable(initial.mainH)
    val mainR = Animatable(initial.mainR)
    /** Uniform swell (1 = none), the "pressure" before flowing open. */
    val swell = Animatable(1f)
    /** Landing squash: >1 widens, and height is scaled by [squashY]. */
    val squashX = Animatable(1f)
    val squashY = Animatable(1f)
    /** Landing sink in dp. */
    val sink = Animatable(0f)

    // Companion droplet (plain state so paths can be driven from one animation callback)
    var secX by mutableFloatStateOf(initial.secX)
    var secW by mutableFloatStateOf(initial.secW)
    var secR by mutableFloatStateOf(initial.secR)
    /** Arc offset in dp (negative = up). */
    var secArc by mutableFloatStateOf(0f)
    /** Extra length (dp) on each side: positive bulges out, negative pinches in. */
    var secLeadLeft by mutableFloatStateOf(0f)
    var secLeadRight by mutableFloatStateOf(0f)
    var secAlpha by mutableFloatStateOf(if (initial.hasSecondary) 1f else 0f)

    // Content and liquid
    val expandedAlpha = Animatable(0f)
    val collapsedAlpha = Animatable(1f)
    /** 0..1 strength of the liquid (blur + threshold) layer; only on while things move. */
    val liquid = Animatable(0f)

    var busy = false
        private set

    /** 0 = pill at rest, 1 = fully expanded; drives the droplet left at the camera. */
    fun expandProgress(t: IslandMotionTargets, expandedH: Float): Float {
        val span = (expandedH - t.pillH)
        if (span <= 0f) return 0f
        return ((mainH.value - t.pillH) / span).coerceIn(0f, 1f)
    }

    suspend fun expand(targets: () -> IslandMotionTargets) = exclusive {
        val t = targets()
        coroutineScope {
            launch { liquid.animateTo(1f, tween(90)) }
            launch { collapsedAlpha.animateTo(0f, tween(90)) }

            // 1. Swell: a little internal pressure before it flows.
            launch {
                swell.animateTo(1.06f, tween(90, easing = FastOutSlowInEasing))
                swell.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 380f))
            }

            // 2. Flow open: width first with a touch of overshoot (surface tension),
            //    height half a beat later, and the top edge last so it grows downward.
            launch { delay(70); mainW.animateTo(t.mainW, spring(dampingRatio = 0.68f, stiffness = 170f)) }
            launch { delay(110); mainH.animateTo(targets().mainH, spring(dampingRatio = 0.74f, stiffness = 190f)) }
            launch { delay(70); mainR.animateTo(t.mainR, spring(dampingRatio = 1f, stiffness = 200f)) }
            launch { delay(70); mainX.animateTo(t.mainX, spring(dampingRatio = 0.85f, stiffness = 220f)) }
            launch { delay(130); mainY.animateTo(t.mainY, spring(dampingRatio = 1f, stiffness = 160f)) }

            // 3. Content surfaces from the liquid once the shape has mostly opened.
            launch { delay(300); expandedAlpha.animateTo(1f, tween(220, easing = FastOutSlowInEasing)) }

            // 4. The companion is drawn into the vacated spot, 100ms after the main moves.
            if (t.hasSecondary) {
                launch {
                    delay(170)
                    relayCompanionToCenter(t)
                }
            }
        }
        liquid.animateTo(0f, tween(160))
    }

    private suspend fun relayCompanionToCenter(t: IslandMotionTargets) = coroutineScope {
        val startX = secX
        val endX = t.secX
        val towardLeft = endX < startX
        val stretch = t.circle * 0.15f
        animate(0f, 1f, animationSpec = tween(360, easing = FastOutSlowInEasing)) { p, _ ->
            val bump = sin(PI * p).toFloat()
            secX = startX + (endX - startX) * p
            secArc = -2f * bump // curve slightly inward (up)
            // The side facing the destination is pulled ahead; the other follows.
            if (towardLeft) secLeadLeft = stretch * bump else secLeadRight = stretch * bump
        }
        secArc = 0f
        secLeadLeft = 0f
        secLeadRight = 0f
        // Arrived: spread out into the regular island shape.
        launch { animate(secW, t.secW, animationSpec = spring(0.72f, 360f)) { v, _ -> secW = v } }
        launch { animate(secR, t.secR, animationSpec = spring(1f, 360f)) { v, _ -> secR = v } }
    }

    suspend fun collapse(targets: () -> IslandMotionTargets, haptics: () -> Unit) = exclusive {
        val t = targets()
        coroutineScope {
            launch { liquid.animateTo(1f, tween(80)) }
            // 1. Content fades first.
            launch { expandedAlpha.animateTo(0f, tween(100)) }

            // 2. Flow back: accelerating toward the center, outer edges trailing.
            val inward = CubicBezierEasing(0.45f, 0f, 0.9f, 0.55f)
            val returnMs = 320
            launch { delay(60); mainH.animateTo(t.mainH, tween(returnMs - 20, easing = inward)) }
            launch { delay(60); mainY.animateTo(t.mainY, tween(returnMs - 40, easing = inward)) }
            launch { delay(60); mainR.animateTo(t.mainR, tween(returnMs - 20, easing = inward)) }
            launch { delay(60); mainX.animateTo(t.mainX, tween(returnMs, easing = inward)) }
            launch { delay(90); mainW.animateTo(t.mainW, tween(returnMs - 30, easing = inward)) }

            // 3. Companion gets squeezed, then popped out ~50ms before the landing.
            val landAt = 60L + returnMs
            if (t.hasSecondary) {
                launch {
                    delay(landAt - 50 - 90)
                    squeezeAndPopCompanion(t)
                }
            }

            // 4. The weighted "thud": sink, spread, flatten, one small rebound.
            launch {
                delay(landAt)
                haptics()
                launch { sink.animateTo(2.5f, tween(70, easing = FastOutSlowInEasing)); sink.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 520f)) }
                launch { squashX.animateTo(1.08f, tween(70, easing = FastOutSlowInEasing)); squashX.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 520f)) }
                launch { squashY.animateTo(0.9f, tween(70, easing = FastOutSlowInEasing)); squashY.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 520f)) }
                launch { delay(40); collapsedAlpha.animateTo(1f, tween(160)) }
            }
        }
        liquid.animateTo(0f, tween(140))
    }

    private suspend fun squeezeAndPopCompanion(t: IslandMotionTargets) = coroutineScope {
        val pushRight = t.secX >= secX
        val squeeze = secW * 0.12f
        // Squeeze: the side facing the incoming main narrows, the far side bulges.
        animate(0f, 1f, animationSpec = tween(90, easing = FastOutSlowInEasing)) { v, _ ->
            if (pushRight) {
                secLeadLeft = -squeeze * v
                secLeadRight = squeeze * 0.6f * v
            } else {
                secLeadRight = -squeeze * v
                secLeadLeft = squeeze * 0.6f * v
            }
        }
        // Pop: pushed out along a slightly outward arc, overshooting once before surface
        // tension pulls it back into its slot.
        val startX = secX
        val pop = spring<Float>(dampingRatio = 0.6f, stiffness = 500f)
        launch { animate(secLeadLeft, 0f, animationSpec = pop) { v, _ -> secLeadLeft = v } }
        launch { animate(secLeadRight, 0f, animationSpec = pop) { v, _ -> secLeadRight = v } }
        launch { animate(secW, t.secW, animationSpec = spring(0.6f, 520f)) { v, _ -> secW = v } }
        launch { animate(secR, t.secR, animationSpec = spring(1f, 520f)) { v, _ -> secR = v } }
        launch {
            animate(startX, t.secX, animationSpec = spring(0.55f, 420f)) { v, _ ->
                secX = v
                val span = t.secX - startX
                val p = if (span == 0f) 1f else ((v - startX) / span).coerceIn(0f, 1f)
                secArc = 3f * sin(PI * p).toFloat() // bulge slightly outward (down)
            }
            secArc = 0f
        }
    }

    /** Gentle settle for target changes outside the choreographies (remeasure, hiding, etc). */
    suspend fun settle(t: IslandMotionTargets, expanded: Boolean) = coroutineScope {
        val s = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)
        launch { mainX.animateTo(t.mainX, s) }
        launch { mainY.animateTo(t.mainY, s) }
        launch { mainW.animateTo(t.mainW, s) }
        launch { mainH.animateTo(t.mainH, s) }
        launch { mainR.animateTo(t.mainR, s) }
        launch { expandedAlpha.animateTo(if (expanded) 1f else 0f, tween(160)) }
        launch { collapsedAlpha.animateTo(if (expanded || t.mainW <= 0f) 0f else 1f, tween(160)) }
        launch { animate(secW, t.secW, animationSpec = s) { v, _ -> secW = v } }
        launch { animate(secR, t.secR, animationSpec = s) { v, _ -> secR = v } }
        if (t.hasSecondary && secAlpha < 0.5f) {
            // A new companion pops out of the main droplet into its slot.
            launch {
                secX = t.mainX
                launch { liquid.animateTo(1f, tween(60)) }
                launch { animate(secAlpha, 1f, animationSpec = tween(120)) { v, _ -> secAlpha = v } }
                animate(t.mainX, t.secX, animationSpec = spring(0.55f, 420f)) { v, _ -> secX = v }
                liquid.animateTo(0f, tween(140))
            }
        } else if (!t.hasSecondary && secAlpha > 0f) {
            // The companion is absorbed back into the main droplet.
            launch {
                launch { liquid.animateTo(1f, tween(60)) }
                animate(secX, t.mainX, animationSpec = spring(0.85f, 380f)) { v, _ -> secX = v }
                animate(secAlpha, 0f, animationSpec = tween(80)) { v, _ -> secAlpha = v }
                liquid.animateTo(0f, tween(120))
            }
        } else {
            launch { animate(secX, t.secX, animationSpec = s) { v, _ -> secX = v } }
        }
    }

    private suspend inline fun exclusive(crossinline block: suspend () -> Unit) {
        busy = true
        try {
            block()
        } finally {
            busy = false
        }
    }
}

/** A short, low "thud" for the landing; falls back to a light tick. */
fun playLandingThud(context: Context) {
    runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val vibrator = (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator ?: return
        if (!vibrator.hasVibrator()) return
        val effect = if (vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_THUD)) {
            VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 0.55f)
                .compose()
        } else {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        }
        vibrator.vibrate(effect)
    }
}
