/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.ui.utils.ExtendedValueAnimator
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer

import kotlin.math.max


class GestureBriefRenderer(
    briefView: View,
    viewStyle: GestureBriefRendererStyle,
) : ItemBriefRenderer<GestureBriefRendererStyle>(briefView, viewStyle) {

    /** True while a waypoint is being dragged — suppresses the traveller so it doesn't jump. */
    var isActivelyDragging: Boolean = false

    private val gestureAnimator: ExtendedValueAnimator = ExtendedValueAnimator.ofFloat(0f, 1f).apply {
        startDelay = 250
        repeatDelay = 1000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener {
            animatedRatio = it.animatedValue as Float
            invalidateView()
        }
    }

    private val progressBorderPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = viewStyle.backgroundColor
        strokeWidth = viewStyle.innerRadiusPx * 0.75f
    }

    private var fingerPaths: List<GestureFingerPath> = emptyList()
    private var waypointGradientPaints: List<List<Paint>> = emptyList()

    private var animatedRatio: Float = 0f
    private var gestureTotalDurationMs: Long = 0L
    private val travelerPos = PointF()

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is GestureDescription) return

        fingerPaths = description.fingers
        gestureTotalDurationMs = description.totalDurationMs
        if (animate) updateAnimator(description.totalDurationMs)
    }

    private fun updateAnimator(maxDurationMs: Long) {
        if (maxDurationMs == 0L || fingerPaths.none { it.animationSegments.isNotEmpty() }) {
            gestureAnimator.cancel()
            animatedRatio = 0f
            return
        }
        val durationMs = max(maxDurationMs, MINIMAL_ANIMATION_DURATION_MS)
        if (!gestureAnimator.isStarted) {
            gestureAnimator.duration = durationMs
            gestureAnimator.start()
        } else if (gestureAnimator.duration != durationMs) {
            gestureAnimator.cancel()
            gestureAnimator.duration = durationMs
            gestureAnimator.start()
        }
    }

    /** Updates finger paths during interactive drag to reflect the moved waypoint without visual jumps. */
    fun updateFingerPaths(paths: List<GestureFingerPath>) {
        fingerPaths = paths
        invalidateView()
    }

    override fun onInvalidate() {
        waypointGradientPaints = fingerPaths.map { path ->
            path.waypoints.map { waypoint ->
                val color = if (waypoint.isEditable) viewStyle.backgroundColor
                            else (viewStyle.fixedBackgroundColor ?: viewStyle.backgroundColor)
                Paint().apply {
                    shader = createRadialGradientShader(
                        position = waypoint.position,
                        radius = viewStyle.outerRadiusPx * 1.75f,
                        color = color,
                    )
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        fingerPaths.forEachIndexed { index, path ->
            val waypoints = path.waypoints
            if (waypoints.isEmpty()) return@forEachIndexed
            val fingerGradients = waypointGradientPaints.getOrNull(index)

            for (i in 1 until waypoints.size) {
                canvas.drawLine(
                    waypoints[i - 1].position.x, waypoints[i - 1].position.y,
                    waypoints[i].position.x, waypoints[i].position.y,
                    viewStyle.linePaint,
                )
            }

            waypoints.forEachIndexed { wpIndex, waypoint ->
                canvas.drawSelectorCircle(waypoint.position, fingerGradients?.getOrNull(wpIndex), waypoint.isEditable)
            }

            if (!isActivelyDragging && path.animationSegments.isNotEmpty() && gestureTotalDurationMs > 0L) {
                val globalTimeMs = animatedRatio * gestureTotalDurationMs
                val fingerLocalTimeMs = (globalTimeMs - path.startTimeMs)
                    .coerceIn(0f, path.durationMs.toFloat())

                if (computeTravelerPosition(path, fingerLocalTimeMs)) {
                    canvas.drawCircle(travelerPos.x, travelerPos.y, viewStyle.innerRadiusPx * 2, viewStyle.travelerPaint ?: viewStyle.innerPaint)
                    canvas.drawCircle(travelerPos.x, travelerPos.y, viewStyle.innerRadiusPx * 2, progressBorderPaint)
                }
            }
        }
    }

    override fun onStop() {
        gestureAnimator.cancel()
        fingerPaths = emptyList()
        waypointGradientPaints = emptyList()
        animatedRatio = 0f
        gestureTotalDurationMs = 0L
    }

    private fun computeTravelerPosition(path: GestureFingerPath, fingerLocalTimeMs: Float): Boolean {
        if (path.animationSegments.isEmpty()) return false

        var elapsed = 0f
        for (segment in path.animationSegments) {
            if (fingerLocalTimeMs <= elapsed + segment.durationMs) {
                when (segment) {

                    is GestureAnimationSegment.Move -> {
                        val t = if (segment.durationMs > 0L) (fingerLocalTimeMs - elapsed) / segment.durationMs else 1f
                        val from = path.waypoints.getOrNull(segment.fromWpIdx)?.position
                        val to = path.waypoints.getOrNull(segment.toWpIdx)?.position

                        if (from != null && to != null) {
                            travelerPos.x = from.x + (to.x - from.x) * t
                            travelerPos.y = from.y + (to.y - from.y) * t
                        }
                    }

                    is GestureAnimationSegment.Wait ->
                        path.waypoints.getOrNull(segment.wpIdx)?.position?.let { travelerPos.set(it) }
                }
                return true
            }

            elapsed += segment.durationMs
        }

        path.waypoints.lastOrNull()?.position?.let { travelerPos.set(it) }
        return true
    }

    private fun Canvas.drawSelectorCircle(position: PointF, backgroundPaint: Paint?, isEditable: Boolean) {
        val outerP = if (isEditable) viewStyle.outerPaint else (viewStyle.fixedOuterPaint ?: viewStyle.outerPaint)
        val innerP = if (isEditable) viewStyle.innerPaint else (viewStyle.fixedInnerPaint ?: viewStyle.innerPaint)
        backgroundPaint?.let { drawCircle(position.x, position.y, viewStyle.outerRadiusPx * 2f, it) }
        drawCircle(position.x, position.y, viewStyle.outerRadiusPx, outerP)
        drawCircle(position.x, position.y, viewStyle.innerRadiusPx, innerP)
    }
}

/**
 * A gesture to render, with all waypoint positions already resolved to screen coordinates.
 * Whatever its source (a live recording, a stored action, an edition in progress), a gesture must be
 * expressed with those types to be displayed.
 */
data class GestureDescription(
    val fingers: List<GestureFingerPath>,
    val totalDurationMs: Long,
) : ItemBriefDescription

data class GestureFingerPath(
    val waypoints: List<GestureWaypoint>,
    val startTimeMs: Long = 0L,
    val durationMs: Long = 0L,
    val animationSegments: List<GestureAnimationSegment> = emptyList(),
)

data class GestureWaypoint(
    val position: PointF,
    val isEditable: Boolean = true,
    /** Identifier of the gesture motion this waypoint originates from, if any. Used to map an edition back to it. */
    val motionId: Identifier? = null,
)

sealed class GestureAnimationSegment {
    abstract val durationMs: Long
    data class Move(val fromWpIdx: Int, val toWpIdx: Int, override val durationMs: Long) : GestureAnimationSegment()
    data class Wait(val wpIdx: Int, override val durationMs: Long) : GestureAnimationSegment()
}

// ---- Style ----

data class GestureBriefRendererStyle(
    @field:ColorInt val backgroundColor: Int,
    val outerPaint: Paint,
    val innerPaint: Paint,
    val linePaint: Paint,
    val outerRadiusPx: Float,
    val innerRadiusPx: Float,
    /** When non-null, used for non-editable (fixed) waypoint outer ring instead of [outerPaint]. */
    val fixedOuterPaint: Paint? = null,
    /** When non-null, used for non-editable (fixed) waypoint inner dot instead of [innerPaint]. */
    val fixedInnerPaint: Paint? = null,
    /** When non-null, used as the glow gradient colour for non-editable waypoints instead of [backgroundColor]. */
    @field:ColorInt val fixedBackgroundColor: Int? = null,
    /** When non-null, used for the traveller fill circle instead of [innerPaint]. */
    val travelerPaint: Paint? = null,
)

private const val MINIMAL_ANIMATION_DURATION_MS = 250L
