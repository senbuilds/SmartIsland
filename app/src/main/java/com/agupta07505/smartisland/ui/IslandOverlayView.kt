/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import com.agupta07505.smartisland.util.CameraAnchor
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.model.SwipeAction
import com.agupta07505.smartisland.ui.expanded.IslandExpandedContent
import com.agupta07505.smartisland.ui.expanded.trySendFirstAction
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.exp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.Canvas
import androidx.annotation.RequiresApi
import android.os.Build
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.data.LaunchableApp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun IslandOverlayView(
    settings: SmartIslandSettings,
    expanded: Boolean,
    notifications: List<IslandNotification>,
    selectedIndex: Int,
    launcherApps: List<LaunchableApp>?,
    onPageSelected: (Int) -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onLaunchApp: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    onDismissNotification: () -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit = {},
    statusBarHeight: Float,
    modifier: Modifier = Modifier,
    isInputActive: Boolean = false,
    onReplyStateChanged: (Boolean) -> Unit = {},
    onDismissAllNotifications: () -> Unit = {},
    isFullWidth: Boolean = true,
    cameraAnchor: CameraAnchor? = null
) {
    // The collapsed island is pinned over the physical camera; the manual offset is only a fallback.
    @Suppress("NAME_SHADOWING")
    val settings = if (cameraAnchor != null) settings.copy(xOffset = cameraAnchor.xOffsetDp) else settings
    val cameraKeepoutDp = cameraAnchor?.holeWidthDp ?: 0f
    // Fix #1: rememberUpdatedState ensures the lambda is always fresh
    // even though pointerInput(Unit) never restarts its coroutine
    val currentOnToggle by rememberUpdatedState(onToggleExpanded)
    val currentOnDismiss by rememberUpdatedState(onDismissNotification)
    val currentOnDismissAll by rememberUpdatedState(onDismissAllNotifications)
    val currentOnOpenFloatingWindow by rememberUpdatedState(onOpenFloatingWindow)
    val currentOnOpenNotificationShade by rememberUpdatedState(onOpenNotificationShade)
    val currentOnOpenNotification by rememberUpdatedState(onOpenNotification)
    val currentOnPageSelected by rememberUpdatedState(onPageSelected)
    val currentExpanded by rememberUpdatedState(expanded)
    val currentSettings by rememberUpdatedState(settings)
    val safeIndex = selectedIndex.coerceIn(0, (notifications.size - 1).coerceAtLeast(0))
    val currentNotifications by rememberUpdatedState(notifications)
    val currentSelectedIndex by rememberUpdatedState(safeIndex)
    val haptic = LocalHapticFeedback.current

    val scope = rememberCoroutineScope()
    var dragOffset by remember { mutableStateOf(0f) }
    var pillDragOffsetX by remember { mutableStateOf(0f) }
    // A pill swipe can expand/collapse the island mid-gesture, which cancels the gesture
    // before its spring-back runs; never carry a stale drag offset across a state change.
    LaunchedEffect(expanded, notifications.size) {
        pillDragOffsetX = 0f
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val displayMetrics = context.resources.displayMetrics
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenCenter = screenWidth / 2f
    val expandedWidth = calculateExpandedWidth(
        isLandscape = isLandscape,
        screenWidthDp = configuration.screenWidthDp.toFloat(),
        screenHeightDp = configuration.screenHeightDp.toFloat()
    ).dp
    // The service grows the window to full screen when expanding, but WindowManager applies
    // that asynchronously. Start the expand morph only once this view is actually full
    // size, otherwise its first ~100ms are clipped by the small window and dropped.
    var windowHeightPx by remember { mutableIntStateOf(0) }
    val windowIsFull = windowHeightPx > with(density) { FULL_WINDOW_MIN_HEIGHT_DP.dp.roundToPx() }
    val targetExpanded = expanded && windowIsFull

    val activeNotification = notifications.getOrNull(safeIndex)
    val activeMode = activeNotification?.mode ?: IslandMode.Empty

    val initialEstimatedHeight = remember(activeMode, notifications.isEmpty()) {
        if (notifications.isEmpty()) 135.dp else defaultEstimatedHeightForMode(activeMode)
    }
    var expandedHeight by remember { mutableStateOf(initialEstimatedHeight) }

    LaunchedEffect(activeMode, notifications.isEmpty()) {
        if (!expanded) {
            expandedHeight = if (notifications.isEmpty()) 135.dp else defaultEstimatedHeightForMode(activeMode)
        }
    }

    val compactGap = COMPACT_INDICATOR_GAP_DP.dp
    val miniPillWidth = settings.width.dp
    val circleSize = settings.height.dp
    val compactShapes = compactNotificationShapes(notifications.size, expanded)
    val hasCompanion = if (settings.enableNotchMode) false else notifications.size >= 2
    val isCircleLeft = settings.circlePosition == SmartIslandSettings.CIRCLE_POSITION_LEFT

    val desiredMainLeft = screenCenter + settings.xOffset.dp - settings.width.dp / 2f
    // The main pill never shifts to make room for the companion; it stays over the camera.
    val minMainLeft = compactGap
    val maxMainLeft = (screenWidth - compactGap - settings.width.dp).coerceAtLeast(compactGap)
    val collapsedMainLeft = desiredMainLeft.coerceIn(minMainLeft, maxMainLeft)
    val mainCenter = collapsedMainLeft + settings.width.dp / 2f
    val circleLeft = if (isCircleLeft) {
        collapsedMainLeft - compactGap - circleSize
    } else {
        collapsedMainLeft + settings.width.dp + compactGap
    }
    val circleCenter = circleLeft + circleSize / 2f

    val collapsedMainOffset = if (settings.enableNotchMode) {
        settings.xOffset.dp
    } else if (isFullWidth) {
        mainCenter - screenCenter
    } else {
        // The service centers the collapsed window on the main pill.
        0.dp
    }
    val expandedTopOffset = calculateExpandedTopOffset(
        enableNotchMode = settings.enableNotchMode,
        hasCompanion = hasCompanion,
        statusBarHeightDp = statusBarHeight,
        notchHeightDp = settings.height,
        circleSizeDp = settings.height,
        compactGapDp = COMPACT_INDICATOR_GAP_DP
    ).dp
    val isIdleHiding = settings.hideWhenIdle && notifications.isEmpty()

    var isAutoHidden by remember { mutableStateOf(false) }
    var userInteractionTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    // Reset auto-hide whenever active notifications change or selection changes
    LaunchedEffect(notifications.map { it.key }, selectedIndex) {
        isAutoHidden = false
        userInteractionTimestamp = System.currentTimeMillis()
    }

    // Auto-hide countdown timer when pill is collapsed and autoHidePill is enabled
    LaunchedEffect(expanded, settings.autoHidePill, settings.autoHideTimeoutSeconds, userInteractionTimestamp) {
        if (expanded || !settings.autoHidePill) {
            isAutoHidden = false
            return@LaunchedEffect
        }
        val timeoutMs = (settings.autoHideTimeoutSeconds.coerceAtLeast(1) * 1000L)
        kotlinx.coroutines.delay(timeoutMs)
        isAutoHidden = true
    }

    val isHiding = isIdleHiding || (settings.autoHidePill && isAutoHidden)
    val pillBackgroundColor = Color(settings.pillColor)

    // A plain notification expands below the status bar with even side margins. The Fold
    // cover screen has nearly square corners (~9dp), so a large iOS-style radius looks off.
    val isIosNotification = activeMode == IslandMode.Notification && !settings.enableNotchMode && !isLandscape
    // Tactile spring scale bounce animation only when user switches between active notifications
    val switchScaleAnim = remember { androidx.compose.animation.core.Animatable(1f) }
    var isInitialComposition by remember { mutableStateOf(true) }
    var lastSelectedIndex by remember { mutableStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        if (isInitialComposition) {
            isInitialComposition = false
            lastSelectedIndex = selectedIndex
            return@LaunchedEffect
        }
        if (lastSelectedIndex != selectedIndex) {
            lastSelectedIndex = selectedIndex
            switchScaleAnim.animateTo(
                targetValue = 0.92f,
                animationSpec = tween(40, easing = FastOutSlowInEasing)
            )
            switchScaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = 650f
                )
            )
        }
    }

    // Dual Pill (Multi-Tasking Split Island) Detection:
    // When 2 or more notifications exist (e.g. Music + Notification/Timer/Call), split into Main Pill + Secondary Bubble
    val secondaryNotification = if (!settings.enableNotchMode && notifications.size >= 2) {
        notifications.firstOrNull { it.key != activeNotification?.key }
    } else null
    val secondaryIndex = if (secondaryNotification != null) {
        notifications.indexOfFirst { it.key == secondaryNotification.key }
    } else -1
    val tertiaryNotification = if (!settings.enableNotchMode && notifications.size >= 3) {
        notifications.firstOrNull {
            it.key != activeNotification?.key && it.key != secondaryNotification?.key
        }
    } else null
    val tertiaryIndex = if (tertiaryNotification != null) {
        notifications.indexOfFirst { it.key == tertiaryNotification.key }
    } else -1
    val isSplitMode = if (settings.enableNotchMode) false else secondaryNotification != null
    val showTertiaryPill = compactShapes.size == 2 && tertiaryNotification != null

    val tertiaryAlpha by animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "tertiaryAlpha"
    )
    val tertiaryScale by animateFloatAsState(
        targetValue = if (showTertiaryPill && !isHiding) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 480f),
        label = "tertiaryScale"
    )

    val expandedCompactX = collapsedMainLeft
    val collapsedSecondaryOffset = if (isFullWidth) {
        circleCenter - screenCenter
    } else {
        circleCenter - mainCenter
    }

    // ---- Liquid motion: one choreographer drives every surface (see IslandMotion) ----
    val expandedMainWidth = if (isIosNotification) screenWidth - IOS_EXPANDED_SIDE_MARGIN_DP.dp * 2 else expandedWidth
    val targets = IslandMotionTargets(
        mainX = if (targetExpanded) 0f else collapsedMainOffset.value,
        mainY = if (targetExpanded) expandedTopOffset.value else 0f,
        mainW = if (targetExpanded) expandedMainWidth.value else if (isHiding) 0f else settings.width,
        mainH = if (targetExpanded) expandedHeight.value else if (isHiding) 0f else settings.height,
        mainR = if (targetExpanded) (if (isIosNotification) IOS_EXPANDED_CORNER_DP else 34f) else if (isHiding) 0f else settings.cornerRadius,
        pillW = settings.width,
        pillH = settings.height,
        hasSecondary = isSplitMode && !isHiding,
        // Expanded: the companion takes over the pill's spot over the camera.
        secX = if (targetExpanded) collapsedMainOffset.value else collapsedSecondaryOffset.value,
        secW = if (targetExpanded) settings.width else circleSize.value,
        secR = if (targetExpanded) settings.cornerRadius else circleSize.value / 2f,
        circle = circleSize.value,
    )
    val motion = remember { IslandMotion(targets) }
    val latestTargets by rememberUpdatedState(targets)
    var choreographedExpanded by remember { mutableStateOf(targetExpanded) }
    LaunchedEffect(targetExpanded) {
        if (targetExpanded == choreographedExpanded) return@LaunchedEffect
        choreographedExpanded = targetExpanded
        if (targetExpanded) {
            motion.expand { latestTargets }
        } else if (latestTargets.mainW > 0f) {
            motion.collapse({ latestTargets }) { playLandingThud(context) }
        }
        motion.settle(latestTargets, targetExpanded)
    }
    LaunchedEffect(targets) {
        if (motion.busy || targetExpanded != choreographedExpanded) return@LaunchedEffect
        motion.settle(targets, targetExpanded)
    }

    val safeWidth = motion.mainW.value.coerceAtLeast(0f).dp
    val safeHeight = motion.mainH.value.coerceAtLeast(0f).dp
    val safeRadius = motion.mainR.value.coerceAtLeast(0f).dp
    val yOffset = motion.mainY.value.dp
    val animatedXOffset = motion.mainX.value.dp
    val collapsedAlpha = motion.collapsedAlpha.value
    val expandedAlpha = motion.expandedAlpha.value
    val contentScale = 0.92f + 0.08f * expandedAlpha
    val contentSlideY = (-6).dp * (1f - expandedAlpha)
    val morphProgress = motion.expandProgress(targets, expandedHeight.value)
    val secondaryAlpha = motion.secAlpha
    val secondaryScale = 1f
    val secondaryOffset = motion.secX.dp
    val secondaryBubbleWidth = motion.secW.dp
    val secondaryBubbleCorner = motion.secR.coerceAtLeast(0f).dp
    val secondaryPillProgress = ((motion.secW - circleSize.value) / (settings.width - circleSize.value))
        .takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f

    // Liquid layer: every black surface is drawn into one layer. While shapes move, the
    // layer is blurred and alpha-thresholded so nearby shapes fuse like two droplets
    // (a metaball effect); once settled the effect is off and edges are crisp again.
    val useLiquid = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !settings.enableNotchMode
    val liquidStrength = if (useLiquid) motion.liquid.value else 0f
    val surfaceColor = pillBackgroundColor.copy(alpha = settings.opacity)

    // Outer Box: Fills the entire WindowManager window bounds (which are padded for easy touch)
    val outerModifier = if (currentExpanded) {
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    if (isInputActive) {
                        onReplyStateChanged(false)
                    }
                    currentOnToggle()
                }
            }
    } else {
        modifier.fillMaxSize()
    }

    Box(
        modifier = outerModifier.onSizeChanged { windowHeightPx = it.height },
        contentAlignment = Alignment.TopCenter
    ) {

        if (useLiquid) {
            val liquidHeight = maxOf(yOffset + safeHeight, circleSize) + LIQUID_MAX_BLUR_DP.dp * 3 + 24.dp
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(liquidHeight)
                    .graphicsLayer {
                        val blurPx = LIQUID_MAX_BLUR_DP.dp.toPx() * liquidStrength
                        renderEffect = if (blurPx > 0.5f) liquidRenderEffect(blurPx) else null
                    }
            ) {
                fun surface(centerX: Float, top: Float, w: Float, h: Float, r: Float, scale: Float, alpha: Float = 1f) {
                    if (w <= 0f || h <= 0f || scale <= 0f || alpha <= 0f) return
                    val sw = w * scale
                    val sh = h * scale
                    drawRoundRect(
                        color = surfaceColor.copy(alpha = surfaceColor.alpha * alpha),
                        topLeft = Offset(centerX - sw / 2f, top + (h - sh) / 2f),
                        size = Size(sw, sh),
                        cornerRadius = CornerRadius(minOf(r * scale, sh / 2f))
                    )
                }
                val cx = size.width / 2f
                val pillW = settings.width.dp.toPx()
                val pillH = settings.height.dp.toPx()

                // The droplet left behind at the camera: it is the pill at rest and
                // shrinks away as the island stretches out (and grows back on collapse).
                if (!isHiding) {
                    surface(
                        centerX = cx + collapsedMainOffset.toPx(),
                        top = 0f,
                        w = pillW,
                        h = pillH,
                        r = settings.cornerRadius.dp.toPx(),
                        scale = (1f - morphProgress).coerceIn(0f, 1f)
                    )
                }

                // Main island: swell (centered), landing squash (anchored at the top) and sink.
                run {
                    val sw = safeWidth.toPx() * motion.swell.value * motion.squashX.value
                    val sh = safeHeight.toPx() * motion.swell.value * motion.squashY.value
                    val grow = safeHeight.toPx() * (motion.swell.value - 1f) / 2f
                    surface(
                        centerX = cx + animatedXOffset.toPx() + (if (!currentExpanded) pillDragOffsetX else 0f),
                        top = yOffset.toPx() + dragOffset + motion.sink.value.dp.toPx() - grow,
                        w = sw,
                        h = sh,
                        r = safeRadius.toPx() * motion.swell.value,
                        scale = switchScaleAnim.value
                    )
                }

                if (secondaryAlpha > 0f && secondaryNotification != null) {
                    // Leading/trailing stretch makes the companion pull toward where it is
                    // going (or pinch where it is being pushed) without distorting content.
                    val leadL = motion.secLeadLeft.dp.toPx()
                    val leadR = motion.secLeadRight.dp.toPx()
                    surface(
                        centerX = cx + secondaryOffset.toPx() + (leadR - leadL) / 2f,
                        top = motion.secArc.dp.toPx(),
                        w = secondaryBubbleWidth.toPx() + leadL + leadR,
                        h = circleSize.toPx(),
                        r = secondaryBubbleCorner.toPx(),
                        scale = switchScaleAnim.value,
                        alpha = secondaryAlpha
                    )
                }
                if (tertiaryAlpha > 0f && tertiaryNotification != null) {
                    surface(
                        centerX = cx + (expandedCompactX - screenCenter + miniPillWidth / 2f).toPx(),
                        top = 0f,
                        w = miniPillWidth.toPx(),
                        h = circleSize.toPx(),
                        r = settings.cornerRadius.dp.toPx(),
                        scale = tertiaryScale * switchScaleAnim.value,
                        alpha = tertiaryAlpha
                    )
                }
            }
        }

        // A faint, moving highlight along the top edge, like light on a black droplet.
        // Only while the shape is in motion; at rest the island is plain black.
        if (useLiquid && liquidStrength > 0.02f && safeWidth > 0.dp) {
            Canvas(modifier = Modifier.fillMaxWidth().height(yOffset + safeHeight + 8.dp)) {
                val w = safeWidth.toPx() * motion.swell.value * motion.squashX.value
                val h = safeHeight.toPx() * motion.swell.value * motion.squashY.value
                val left = size.width / 2f + animatedXOffset.toPx() - w / 2f
                val top = yOffset.toPx() + dragOffset + motion.sink.value.dp.toPx()
                val inset = 0.75.dp.toPx()
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.16f * liquidStrength),
                        0.45f to Color.Transparent,
                        startY = top,
                        endY = top + h
                    ),
                    topLeft = Offset(left + inset, top + inset),
                    size = Size((w - inset * 2).coerceAtLeast(0f), (h - inset * 2).coerceAtLeast(0f)),
                    cornerRadius = CornerRadius((safeRadius.toPx() - inset).coerceAtLeast(0f)),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Invisible touch target over the pill location when hiding, so tapping the area reveals the pill or opens shortcuts
        if (isHiding && !currentExpanded) {
            Box(
                modifier = Modifier
                    .width(settings.width.dp)
                    .height(settings.height.dp)
                    .graphicsLayer {
                        translationX = collapsedMainOffset.toPx()
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            if (settings.autoHidePill && isAutoHidden) {
                                // First tap on auto-hidden pill: awaken and reveal the pill
                                isAutoHidden = false
                                userInteractionTimestamp = System.currentTimeMillis()
                            } else if (settings.enableAppShortcuts || currentNotifications.isNotEmpty()) {
                                // Empty notifications idle hiding: expand favorite shortcuts if enabled
                                currentOnToggle()
                            }
                        }
                    }
            )
        }

        val mainShape = if (settings.enableNotchMode && !currentExpanded) {
            RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = safeRadius,
                bottomEnd = safeRadius
            )
        } else {
            RoundedCornerShape(safeRadius)
        }

        // Inner Box: The actual visible pill container, managing the black background shape and size animations
        Box(
            modifier = Modifier
                .width(safeWidth)
                .height(safeHeight)
                .graphicsLayer {
                    // Guard the horizontal slide-in: an unanchored 0.95 scale on a
                    // top-center-aligned box makes a near-zero-width pill drift
                    // left/right at the very start/end of the morph (reads as flicker).
                    val widthPx = size.width
                    translationX = animatedXOffset.toPx() + (if (!currentExpanded) pillDragOffsetX else 0f)
                    scaleX = if (widthPx > 0f) switchScaleAnim.value else 1f
                    scaleY = if (widthPx > 0f) switchScaleAnim.value else 1f
                    translationY = yOffset.toPx() + dragOffset + motion.sink.value.dp.toPx()
                }
                .then(
                    if (settings.enableShadow && settings.shadowElevation > 0f && !isHiding && safeWidth > 0.dp && !useLiquid) {
                        val activeMainShadow = if (currentExpanded) {
                            (settings.shadowElevation * 1.5f).dp
                        } else {
                            settings.shadowElevation.dp
                        }
                        Modifier.shadow(
                            elevation = activeMainShadow,
                            shape = mainShape,
                            clip = false,
                            ambientColor = Color.Black,
                            spotColor = Color.Black
                        )
                    } else Modifier
                )
                .clip(mainShape)
                .then(if (useLiquid) Modifier else Modifier.background(surfaceColor))
                .pointerInput(displayMetrics.density, isInputActive) {
                    if (isInputActive) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        userInteractionTimestamp = System.currentTimeMillis()
                        val pressTimeMs = System.currentTimeMillis()
                        val wasExpandedAtStart = currentExpanded
                        var isHoldRegistered = false
                        var dragAccumulatorY = 0f
                        var dragAccumulatorX = 0f
                        var isDragging = false
                        var pillGestureTriggered = false

                        val holdJob = scope.launch {
                            kotlinx.coroutines.delay(HOLD_GESTURE_THRESHOLD_MS)
                            isHoldRegistered = true
                            triggerHapticVibration(context)
                        }

                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break

                            if (change.changedToUp()) {
                                change.consume()
                                holdJob.cancel()
                                val totalElapsedMs = System.currentTimeMillis() - pressTimeMs
                                val currentNotification = currentNotifications.getOrNull(currentSelectedIndex)

                                if (wasExpandedAtStart) {
                                    val swipeUpThreshold = -SWIPE_THRESHOLD_DP * displayMetrics.density
                                    val swipeDownThreshold = SWIPE_THRESHOLD_DP * displayMetrics.density
                                    if (isDragging && currentSettings.enableSwipeActions && dragAccumulatorY < swipeUpThreshold) {
                                        val isHold = isHoldRegistered || totalElapsedMs >= HOLD_GESTURE_THRESHOLD_MS
                                        val actionStr = if (isHold) currentSettings.swipeHoldUpAction else currentSettings.swipeUpAction
                                        val action = SwipeAction.fromId(actionStr, SwipeAction.Collapse)
                                        executeSwipeAction(
                                            action = action,
                                            currentNotification = currentNotification,
                                            context = context,
                                            onDismiss = currentOnDismiss,
                                            onDismissAll = currentOnDismissAll,
                                            onToggle = currentOnToggle,
                                            onOpenNotification = currentOnOpenNotification,
                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                            onPageSelected = currentOnPageSelected,
                                            notificationsSize = currentNotifications.size,
                                            currentIndex = currentSelectedIndex
                                        )
                                    } else if (isDragging && currentSettings.enableSwipeActions && dragAccumulatorY > swipeDownThreshold) {
                                        val action = SwipeAction.fromId(currentSettings.swipeDownAction, SwipeAction.OpenApp)
                                        executeSwipeAction(
                                            action = action,
                                            currentNotification = currentNotification,
                                            context = context,
                                            onDismiss = currentOnDismiss,
                                            onDismissAll = currentOnDismissAll,
                                            onToggle = currentOnToggle,
                                            onOpenNotification = currentOnOpenNotification,
                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                            onPageSelected = currentOnPageSelected,
                                            notificationsSize = currentNotifications.size,
                                            currentIndex = currentSelectedIndex
                                        )
                                    } else if (!isDragging || (abs(dragAccumulatorY) < 10f * displayMetrics.density && abs(dragAccumulatorX) < 10f * displayMetrics.density)) {
                                        if (!isHoldRegistered) {
                                            if (currentNotification != null) {
                                                currentOnOpenNotification(currentNotification)
                                            } else {
                                                SmartIslandRepositories.notificationRepository(context).resetTimer()
                                            }
                                        }
                                    }
                                } else {
                                    // Collapsed state (In-Pill Gestures)
                                    if (!pillGestureTriggered) {
                                        val isPillSwipeEnabled = currentSettings.enablePillSwipeActions
                                        val pillThreshold = PILL_SWIPE_THRESHOLD_DP * displayMetrics.density
                                        val absX = abs(dragAccumulatorX)
                                        val absY = abs(dragAccumulatorY)

                                        if (isPillSwipeEnabled && (absX >= pillThreshold || absY >= pillThreshold)) {
                                            pillGestureTriggered = true
                                            if (absX > absY) {
                                                if (dragAccumulatorX < -pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                } else if (dragAccumulatorX > pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeRightAction, SwipeAction.NextNotification)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                }
                                            } else {
                                                if (dragAccumulatorY < -pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeUpAction, SwipeAction.None)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                } else if (dragAccumulatorY > pillThreshold) {
                                                    val action = SwipeAction.fromId(currentSettings.pillSwipeDownAction, SwipeAction.Expand)
                                                    executeSwipeAction(
                                                        action = action,
                                                        currentNotification = currentNotification,
                                                        context = context,
                                                        onDismiss = currentOnDismiss,
                                                        onDismissAll = currentOnDismissAll,
                                                        onToggle = currentOnToggle,
                                                        onOpenNotification = currentOnOpenNotification,
                                                        onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                        onOpenNotificationShade = currentOnOpenNotificationShade,
                                                        onPageSelected = currentOnPageSelected,
                                                        notificationsSize = currentNotifications.size,
                                                        currentIndex = currentSelectedIndex
                                                    )
                                                }
                                            }
                                        } else {
                                            // Tap on collapsed pill: expands or reveals
                                            if (!isHoldRegistered) {
                                                if (settings.autoHidePill && isAutoHidden) {
                                                    isAutoHidden = false
                                                    userInteractionTimestamp = System.currentTimeMillis()
                                                } else if (currentNotifications.isNotEmpty() || currentSettings.enableAppShortcuts) {
                                                    currentOnToggle()
                                                }
                                            }
                                        }
                                    }
                                }
                                break
                            } else {
                                val dragAmountY = change.positionChange().y
                                val dragAmountX = change.positionChange().x
                                if (abs(dragAmountY) > 0.5f || abs(dragAmountX) > 0.5f) {
                                    isDragging = true
                                    dragAccumulatorY += dragAmountY
                                    dragAccumulatorX += dragAmountX
                                    change.consume()
                                    if (abs(dragAccumulatorY) > 5f * displayMetrics.density || abs(dragAccumulatorX) > 5f * displayMetrics.density) {
                                        holdJob.cancel()
                                    }
                                    if (wasExpandedAtStart) {
                                        // Rubber band: the card leans toward the finger but never
                                        // travels far enough to cover the status bar.
                                        val maxPull = EXPANDED_DRAG_PULL_DP * displayMetrics.density
                                        dragOffset = sign(dragAccumulatorY) * maxPull *
                                            (1f - exp(-abs(dragAccumulatorY) / (maxPull * 3f)))
                                    } else {
                                        pillDragOffsetX = (dragAccumulatorX * 0.35f).coerceIn(
                                            -24f * displayMetrics.density,
                                            24f * displayMetrics.density
                                        )
                                        dragOffset = (dragAccumulatorY * 0.35f).coerceIn(
                                            -12f * displayMetrics.density,
                                            12f * displayMetrics.density
                                        )

                                        // Snappy immediate execution when swipe threshold is crossed while dragging
                                        if (!pillGestureTriggered && currentSettings.enablePillSwipeActions) {
                                            val pillThreshold = PILL_SWIPE_THRESHOLD_DP * displayMetrics.density
                                            val absX = abs(dragAccumulatorX)
                                            val absY = abs(dragAccumulatorY)

                                            if (absX >= pillThreshold || absY >= pillThreshold) {
                                                pillGestureTriggered = true
                                                holdJob.cancel()
                                                val currentNotification = currentNotifications.getOrNull(currentSelectedIndex)

                                                if (absX > absY) {
                                                    if (dragAccumulatorX < -pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeLeftAction, SwipeAction.PreviousNotification)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    } else if (dragAccumulatorX > pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeRightAction, SwipeAction.NextNotification)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    }
                                                } else {
                                                    if (dragAccumulatorY < -pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeUpAction, SwipeAction.None)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    } else if (dragAccumulatorY > pillThreshold) {
                                                        val action = SwipeAction.fromId(currentSettings.pillSwipeDownAction, SwipeAction.Expand)
                                                        executeSwipeAction(
                                                            action = action,
                                                            currentNotification = currentNotification,
                                                            context = context,
                                                            onDismiss = currentOnDismiss,
                                                            onDismissAll = currentOnDismissAll,
                                                            onToggle = currentOnToggle,
                                                            onOpenNotification = currentOnOpenNotification,
                                                            onOpenFloatingWindow = currentOnOpenFloatingWindow,
                                                            onOpenNotificationShade = currentOnOpenNotificationShade,
                                                            onPageSelected = currentOnPageSelected,
                                                            notificationsSize = currentNotifications.size,
                                                            currentIndex = currentSelectedIndex
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        holdJob.cancel()
                        if (pillDragOffsetX != 0f) {
                            val startPillOffset = pillDragOffsetX
                            scope.launch {
                                androidx.compose.animation.core.Animatable(startPillOffset).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    pillDragOffsetX = value
                                }
                            }
                        }
                        if (dragOffset != 0f) {
                            val startDrag = dragOffset
                            scope.launch {
                                androidx.compose.animation.core.Animatable(startDrag).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) {
                                    dragOffset = value
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.TopCenter
        ) {
            // Collapsed content layer (pinned to fixed pill bounds at top-center, cancelling yOffset)
            if (collapsedAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .width(settings.width.dp)
                        .height(settings.height.dp)
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = collapsedAlpha
                            renderEffect = contentBlur((1f - collapsedAlpha) * CONTENT_MAX_BLUR_DP.dp.toPx())
                        }
                ) {
                    IslandCollapsedContent(
                        mode = activeMode,
                        notification = activeNotification,
                        collapsedAlpha = collapsedAlpha,
                        settings = settings,
                        cameraKeepoutDp = cameraKeepoutDp
                    )
                }
            }

            // Expanded content layer — smoothly fade out while collapsing
            if (expanded || expandedAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer {
                            alpha = expandedAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                            translationY = contentSlideY.toPx()
                            renderEffect = contentBlur((1f - expandedAlpha) * CONTENT_MAX_BLUR_DP.dp.toPx())
                        }
                ) {
                    IslandExpandedContent(
                        notifications = notifications,
                        launcherApps = launcherApps,
                        selectedIndex = selectedIndex,
                        onPageSelected = onPageSelected,
                        onOpenNotification = onOpenNotification,
                        onLaunchApp = onLaunchApp,
                        onCollapse = onToggleExpanded,
                        statusBarHeight = statusBarHeight.dp,
                        // Each mode owns its natural height. The launcher already
                        // supplies its own loading height and must not impose that
                        // minimum on compact call or battery content.
                        onHeightMeasured = { expandedHeight = it },
                        settings = settings,
                        onReplyStateChanged = onReplyStateChanged
                    )
                }
            }
        }

        // Collapsed: secondary circle. Expanded with 2: the same item morphs
        // into a full-size pill. Expanded with 3+: it stays the circle on the right.
        if (!settings.enableNotchMode && secondaryAlpha > 0f && secondaryNotification != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            secondaryOffset.roundToPx(),
                            motion.secArc.dp.roundToPx()
                        )
                    }
                    .width(secondaryBubbleWidth)
                    .height(circleSize)
                    .graphicsLayer {
                        alpha = secondaryAlpha
                        scaleX = secondaryScale * switchScaleAnim.value
                        scaleY = secondaryScale * switchScaleAnim.value
                    }
                    .then(
                        if (settings.enableShadow && settings.shadowElevation > 0f && !useLiquid) {
                            Modifier.shadow(
                                elevation = (settings.shadowElevation * 0.85f).dp,
                                shape = RoundedCornerShape(secondaryBubbleCorner),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(secondaryBubbleCorner))
                    .then(if (useLiquid) Modifier else Modifier.background(surfaceColor))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (secondaryIndex >= 0) {
                            onPageSelected(secondaryIndex)
                        }
                        if (!currentExpanded) {
                            currentOnToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Content swaps only once the shape has settled: the old face
                            // fades early, the new one late, never both at once.
                            alpha = 1f - smoothStep(0f, 0.35f, secondaryPillProgress)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    SecondaryBubbleContent(
                        notification = secondaryNotification,
                        settings = settings
                    )
                }
                Box(
                    modifier = Modifier
                        .requiredWidth(miniPillWidth)
                        .height(circleSize)
                        .graphicsLayer { alpha = smoothStep(0.75f, 1f, secondaryPillProgress) },
                    contentAlignment = Alignment.Center
                ) {
                    IslandCollapsedContent(
                        mode = secondaryNotification.mode,
                        notification = secondaryNotification,
                        collapsedAlpha = 1f,
                        settings = settings
                    )
                }
            }
        }

        if (!settings.enableNotchMode && tertiaryAlpha > 0f && tertiaryNotification != null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            (expandedCompactX - screenCenter + miniPillWidth / 2f).roundToPx(),
                            0
                        )
                    }
                    .width(miniPillWidth)
                    .height(circleSize)
                    .graphicsLayer {
                        alpha = tertiaryAlpha
                        scaleX = tertiaryScale * switchScaleAnim.value
                        scaleY = tertiaryScale * switchScaleAnim.value
                    }
                    .then(
                        if (settings.enableShadow && settings.shadowElevation > 0f && !useLiquid) {
                            Modifier.shadow(
                                elevation = (settings.shadowElevation * 0.85f).dp,
                                shape = RoundedCornerShape(settings.cornerRadius.dp),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black
                            )
                        } else Modifier
                    )
                    .clip(RoundedCornerShape(settings.cornerRadius.dp))
                    .then(if (useLiquid) Modifier else Modifier.background(surfaceColor))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (tertiaryIndex >= 0) {
                            onPageSelected(tertiaryIndex)
                        }
                        if (!currentExpanded) {
                            currentOnToggle()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                IslandCollapsedContent(
                    mode = tertiaryNotification.mode,
                    notification = tertiaryNotification,
                    collapsedAlpha = 1f,
                    settings = settings
                )
            }
        }
    }
}

@Composable
private fun SecondaryBubbleContent(
    notification: IslandNotification,
    settings: SmartIslandSettings
) {
    when (notification.mode) {
        IslandMode.Bluetooth -> {
            BluetoothCollapsedRight(
                notification = notification,
                settings = settings
            )
        }
        IslandMode.Flashlight -> {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.FlashlightOn,
                    contentDescription = "Flashlight",
                    tint = Color(0xFFFACC15),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        IslandMode.Hotspot -> {
            HotspotCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Battery -> {
            BatteryCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.LiveActivity -> {
            LiveActivityCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Navigation -> {
            NavigationCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.IncomingCall -> {
            val icon = notification.largeIcon ?: notification.icon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Rounded.Call,
                    contentDescription = null,
                    tint = Color(settings.callColor),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        IslandMode.Music -> {
            val artwork = notification.largeIcon ?: notification.icon
            if (artwork != null) {
                Image(
                    bitmap = artwork.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(settings.musicVisualizerColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        IslandMode.ScreenRecording -> {
            ScreenRecordingCollapsedGlyph(settings = settings)
        }
        IslandMode.Timer -> {
            TimerCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Stopwatch -> {
            StopwatchCollapsedGlyph(notification = notification, settings = settings)
        }
        IslandMode.Notification, IslandMode.DownloadUpload, IslandMode.Empty -> {
            NotificationGlyph(notification = notification, settings = settings)
        }
    }
}

// Animation specs
internal const val EXPANDED_WIDTH_RATIO = 0.95f

internal fun calculateExpandedWidth(
    isLandscape: Boolean,
    screenWidthDp: Float,
    screenHeightDp: Float,
    ratio: Float = EXPANDED_WIDTH_RATIO
): Float {
    return if (isLandscape) {
        val portraitWidth = minOf(screenWidthDp, screenHeightDp)
        (portraitWidth * ratio).coerceIn(340f, 440f)
    } else {
        screenWidthDp * ratio
    }
}

internal fun calculateExpandedTopOffset(
    enableNotchMode: Boolean,
    hasCompanion: Boolean,
    statusBarHeightDp: Float,
    notchHeightDp: Float = 35f,
    circleSizeDp: Float = 34f,
    compactGapDp: Float = COMPACT_INDICATOR_GAP_DP
): Float {
    return if (enableNotchMode) {
        maxOf(statusBarHeightDp, notchHeightDp) + 8f
    } else if (hasCompanion) {
        maxOf(statusBarHeightDp, circleSizeDp + compactGapDp)
    } else {
        statusBarHeightDp
    }
}

internal fun calculateSecondaryExpandedOffset(
    secondaryIsPill: Boolean,
    isCircleLeft: Boolean,
    isFullWidth: Boolean,
    expandedCompactX: Float,
    screenCenter: Float,
    miniPillWidth: Float,
    circleSize: Float,
    compactGap: Float
): Float {
    return if (secondaryIsPill) {
        val secCenter = expandedCompactX + miniPillWidth / 2f
        if (isFullWidth) secCenter - screenCenter else 0f
    } else if (isCircleLeft) {
        val secCenter = expandedCompactX - compactGap - circleSize / 2f
        // The window is centered on the main pill, so offsets are measured from its center
        if (isFullWidth) secCenter - screenCenter else -(miniPillWidth / 2f + compactGap + circleSize / 2f)
    } else {
        val secCenter = expandedCompactX + miniPillWidth + compactGap + circleSize / 2f
        if (isFullWidth) secCenter - screenCenter else miniPillWidth / 2f + compactGap + circleSize / 2f
    }
}
private const val SWIPE_THRESHOLD_DP = 35f
private const val LIQUID_MAX_BLUR_DP = 6f

private fun smoothStep(from: Float, to: Float, x: Float): Float {
    val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}
private const val FULL_WINDOW_MIN_HEIGHT_DP = 200f
private const val CONTENT_MAX_BLUR_DP = 10f

private fun contentBlur(radiusPx: Float): androidx.compose.ui.graphics.RenderEffect? =
    if (radiusPx > 0.5f) BlurEffect(radiusPx, radiusPx, TileMode.Decal) else null

/** Blur, then push alpha through a steep threshold so overlapping blurs fuse into one smooth shape. */
@RequiresApi(Build.VERSION_CODES.S)
private fun liquidRenderEffect(blurPx: Float): androidx.compose.ui.graphics.RenderEffect {
    val threshold = android.graphics.ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, LIQUID_ALPHA_GAIN, -LIQUID_ALPHA_GAIN * 0.5f * 255f
        )
    )
    val blur = android.graphics.RenderEffect.createBlurEffect(blurPx, blurPx, android.graphics.Shader.TileMode.DECAL)
    return android.graphics.RenderEffect
        .createColorFilterEffect(android.graphics.ColorMatrixColorFilter(threshold), blur)
        .asComposeRenderEffect()
}

private const val LIQUID_ALPHA_GAIN = 18f
private const val IOS_EXPANDED_SIDE_MARGIN_DP = 14f
private const val IOS_EXPANDED_CORNER_DP = 20f
private const val PILL_SWIPE_THRESHOLD_DP = 16f
private const val DRAG_MAX_OFFSET_DP = 100f
private const val EXPANDED_DRAG_PULL_DP = 24f
private const val COMPACT_INDICATOR_GAP_DP = 8f
private const val HOLD_GESTURE_THRESHOLD_MS = 300L

private fun triggerHapticVibration(context: android.content.Context) {
    runCatching {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            val vibrator = vm?.defaultVibrator
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(60L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                return
            }
        }
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(60L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60L)
            }
        }
    }
}

internal enum class CompactNotificationShape { MiniPill, Circle }

internal fun defaultEstimatedHeightForMode(mode: IslandMode?): Dp {
    return when (mode) {
        IslandMode.Music -> 175.dp
        IslandMode.Notification -> 135.dp
        IslandMode.IncomingCall, IslandMode.Battery -> 115.dp
        IslandMode.LiveActivity, IslandMode.Navigation -> 180.dp
        IslandMode.DownloadUpload, IslandMode.Hotspot -> 160.dp
        IslandMode.Bluetooth, IslandMode.Flashlight, IslandMode.ScreenRecording,
        IslandMode.Timer, IslandMode.Stopwatch -> 115.dp
        IslandMode.Empty, null -> 135.dp
    }
}

internal fun compactNotificationShapes(
    notificationCount: Int,
    expanded: Boolean
): List<CompactNotificationShape> = when {
    notificationCount < 2 -> emptyList()
    // Like the Dynamic Island, other activities stay put as a small detached circle while
    // one expands; morphing them across the camera made the expand look jittery.
    else -> listOf(CompactNotificationShape.Circle)
}

private fun trySkipMedia(
    context: android.content.Context?,
    notification: IslandNotification?,
    forward: Boolean
): Boolean {
    if (context == null) return false

    // 1. Direct notification action PendingIntent (e.g. Spotify, YouTube Music, podcasts)
    val actionSent = if (forward) {
        notification.trySendFirstAction(context, "next", "skip", "forward")
    } else {
        notification.trySendFirstAction(context, "previous", "prev", "rewind")
    }
    if (actionSent) return true

    // 2. Notification MediaSession token
    val token = notification?.mediaToken
    if (token != null) {
        val success = runCatching {
            val controller = android.media.session.MediaController(context, token)
            if (forward) controller.transportControls.skipToNext() else controller.transportControls.skipToPrevious()
            true
        }.getOrDefault(false)
        if (success) return true
    }

    // 3. Delegate to NotificationListenerService via SmartIslandCommand
    runCatching {
        val repo = SmartIslandRepositories.notificationRepository(context)
        if (forward) {
            repo.sendCommand(SmartIslandCommand.SkipNext(notification?.packageName))
        } else {
            repo.sendCommand(SmartIslandCommand.SkipPrevious(notification?.packageName))
        }
    }

    // 4. Fallback: AudioManager media key events
    runCatching {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val keyCode = if (forward) android.view.KeyEvent.KEYCODE_MEDIA_NEXT else android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(down)
        audioManager?.dispatchMediaKeyEvent(up)
    }

    return true
}

private fun tryPlayPauseMedia(
    context: android.content.Context?,
    notification: IslandNotification?
) {
    if (context == null) return

    // 1. Direct notification action PendingIntent
    val actionSent = notification.trySendFirstAction(context, "play", "pause", "resume", "toggle")
    if (actionSent) return

    // 2. Notification MediaSession token
    val token = notification?.mediaToken
    if (token != null) {
        val success = runCatching {
            val controller = android.media.session.MediaController(context, token)
            if (notification.mediaIsPlaying) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
            true
        }.getOrDefault(false)
        if (success) return
    }

    // 3. Delegate to NotificationListenerService via SmartIslandCommand
    runCatching {
        val repo = SmartIslandRepositories.notificationRepository(context)
        repo.sendCommand(SmartIslandCommand.PlayPause(notification?.packageName))
    }

    // 4. Fallback: AudioManager media key events
    runCatching {
        val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val keyCode = android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        val down = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
        val up = android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode)
        audioManager?.dispatchMediaKeyEvent(down)
        audioManager?.dispatchMediaKeyEvent(up)
    }
}

internal fun executeSwipeAction(
    action: SwipeAction,
    currentNotification: IslandNotification?,
    context: android.content.Context? = null,
    onDismiss: () -> Unit,
    onDismissAll: () -> Unit,
    onToggle: () -> Unit,
    onOpenNotification: (IslandNotification) -> Unit,
    onOpenFloatingWindow: () -> Unit,
    onOpenNotificationShade: () -> Unit,
    onPageSelected: (Int) -> Unit,
    notificationsSize: Int,
    currentIndex: Int
) {
    if (context != null && action != SwipeAction.None) {
        triggerHapticVibration(context)
    }
    when (action) {
        SwipeAction.DismissCurrent -> onDismiss()
        SwipeAction.DismissAll -> onDismissAll()
        SwipeAction.Collapse -> onToggle()
        SwipeAction.Expand -> onToggle()
        SwipeAction.OpenApp -> {
            if (currentNotification != null) {
                onOpenNotification(currentNotification)
            }
        }
        SwipeAction.FloatingWindow -> onOpenFloatingWindow()
        SwipeAction.NotificationShade -> onOpenNotificationShade()
        SwipeAction.NextPrevious, SwipeAction.NextNotification -> {
            if (notificationsSize > 1) {
                val nextIndex = (currentIndex + 1) % notificationsSize
                onPageSelected(nextIndex)
            } else {
                trySkipMedia(context, currentNotification, forward = true)
            }
        }
        SwipeAction.PreviousNotification -> {
            if (notificationsSize > 1) {
                val prevIndex = (currentIndex - 1 + notificationsSize) % notificationsSize
                onPageSelected(prevIndex)
            } else {
                trySkipMedia(context, currentNotification, forward = false)
            }
        }
        SwipeAction.NextTrack -> {
            trySkipMedia(context, currentNotification, forward = true)
        }
        SwipeAction.PreviousTrack -> {
            trySkipMedia(context, currentNotification, forward = false)
        }
        SwipeAction.PlayPause -> {
            tryPlayPauseMedia(context, currentNotification)
        }
        SwipeAction.None -> {
            // Disabled / No action
        }
    }
}
