/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Rounded rectangle with continuous-curvature corners, in the spirit of Apple's
 * `.continuous` capsules.
 *
 * A circular corner joins its straight edge with a sudden jump in curvature, which the
 * eye reads as a seam. Each corner here is built from a curvature profile instead: the
 * bend starts at zero where it leaves the long top/bottom edge and rises smoothly to its
 * maximum at the side, where it has turned 90°. A capsule's tip therefore stays round
 * while the join into the straight edge is seamless.
 *
 * [smoothing] 0 = plain circular corners (exactly a normal rounded rect), 1 = full ease.
 */
fun continuousRoundRectPath(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
    smoothing: Float,
    path: Path = Path(),
): Path {
    path.reset()
    if (width <= 0f || height <= 0f) return path
    val r = radius.coerceIn(0f, min(width, height) / 2f)
    if (r <= 0f) {
        path.addRect(Rect(left, top, left + width, top + height))
        return path
    }
    val corner = cornerProfile(smoothing.coerceIn(0f, 1f) * MAX_EASE)
    // Scale so the corner drops exactly r vertically; it reaches further along the edge.
    var scale = r / corner.dropY
    if (corner.reachX * scale > width / 2f) scale = (width / 2f) / corner.reachX
    val reach = corner.reachX * scale
    val drop = corner.dropY * scale
    val right = left + width
    val bottom = top + height
    val xs = corner.xs
    val ys = corner.ys
    val last = xs.lastIndex

    // Top edge → top-right corner (join to side)
    path.moveTo(left + reach, top)
    path.lineTo(right - reach, top)
    for (i in 1..last) path.lineTo(right - reach + xs[i] * scale, top + ys[i] * scale)
    // Right side → bottom-right corner (side back to join), mirrored vertically
    path.lineTo(right, bottom - drop)
    for (i in last - 1 downTo 0) path.lineTo(right - reach + xs[i] * scale, bottom - ys[i] * scale)
    // Bottom edge → bottom-left corner
    path.lineTo(left + reach, bottom)
    for (i in 1..last) path.lineTo(left + reach - xs[i] * scale, bottom - ys[i] * scale)
    // Left side → top-left corner
    path.lineTo(left, top + drop)
    for (i in last - 1 downTo 0) path.lineTo(left + reach - xs[i] * scale, top + ys[i] * scale)
    path.close()
    return path
}

/** One corner, from the edge join (0,0) heading +x, turning 90° to head +y. Unit length. */
private class CornerProfile(val xs: FloatArray, val ys: FloatArray) {
    val reachX get() = xs.last()
    val dropY get() = ys.last()
}

private val profileCache = HashMap<Int, CornerProfile>()

/**
 * Curvature along the corner is k(u) = c * ((1 - ease) + ease * (2u - u^2)), u in 0..1:
 * ease 0 is a circle. At ease 1 the curvature is zero at the edge join and peaks with zero
 * slope at the side, so a capsule's mirrored halves meet smoothly. This profile makes the
 * curve start 1.53r from the end, matching the measured reach of Apple's continuous corners.
 */
private fun cornerProfile(ease: Float): CornerProfile {
    val key = (ease * 1000).toInt()
    profileCache[key]?.let { return it }
    val n = PROFILE_STEPS
    fun f(u: Double) = (1 - ease) + ease * (2 * u - u * u)
    // Normalize so the total turn is exactly 90°.
    var area = 0.0
    for (i in 0 until n) area += f((i + 0.5) / n) / n
    val c = (PI / 2) / area
    val xs = FloatArray(n + 1)
    val ys = FloatArray(n + 1)
    var theta = 0.0
    var x = 0.0
    var y = 0.0
    for (i in 0 until n) {
        val u = (i + 0.5) / n
        val dTheta = c * f(u) / n
        val mid = theta + dTheta / 2
        x += cos(mid) / n
        y += sin(mid) / n
        theta += dTheta
        xs[i + 1] = x.toFloat()
        ys[i + 1] = y.toFloat()
    }
    return CornerProfile(xs, ys).also { profileCache[key] = it }
}

private const val MAX_EASE = 1f
private const val PROFILE_STEPS = 40
