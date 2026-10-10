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
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.utils.ExtendedValueAnimator
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer

import kotlin.math.PI
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

    private val labelBandWidthPx: Float = viewStyle.conditionLabelStyle?.let { labelStyle ->
        labelStyle.textPaint.descent() - labelStyle.textPaint.ascent() + labelStyle.paddingPx * 2
    } ?: 0f

    private val defaultWaypointPaints: WaypointPaints = WaypointPaints(
        outer = viewStyle.outerPaint,
        inner = viewStyle.innerPaint,
        fixedOuter = viewStyle.fixedOuterPaint ?: viewStyle.outerPaint,
        fixedInner = viewStyle.fixedInnerPaint ?: viewStyle.innerPaint,
        labelBackground = viewStyle.outerPaint.asLabelBand(labelBandWidthPx),
        fixedLabelBackground = (viewStyle.fixedOuterPaint ?: viewStyle.outerPaint).asLabelBand(labelBandWidthPx),
    )

    private val fingerWaypointPaints: List<WaypointPaints> =
        viewStyle.fingerColors.map(defaultWaypointPaints::withColor)

    private val heldWaypointPaints: WaypointPaints? =
        viewStyle.heldColor?.let(defaultWaypointPaints::withColor)

    private var fingerPaths: List<GestureFingerPath> = emptyList()
    private var waypointGradientPaints: List<List<Paint>> = emptyList()

    private var animatedRatio: Float = 0f
    private var gestureTotalDurationMs: Long = 0L
    private val travelerPos = PointF()
    private val conditionRect = RectF()
    private val labelArcOval = RectF()
    private val labelArcPath = Path()
    /** Labels already laid out along the waypoint circle, by original label. */
    private val labelLayouts: MutableMap<String, LabelLayout> = mutableMapOf()

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
        fingerPaths.forEach { path ->
            path.waypoints.forEach { waypoint ->
                waypoint.condition?.bitmap?.let { canvas.drawConditionBitmap(waypoint.position, it) }
            }
        }

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

            val fingerWaypointPaints = waypointPaints(index)
            waypoints.forEachIndexed { wpIndex, waypoint ->
                val isHeldWaypoint = path.isHeld && wpIndex == waypoints.lastIndex
                canvas.drawSelectorCircle(
                    waypoint.position,
                    fingerGradients?.getOrNull(wpIndex),
                    waypoint.isEditable,
                    if (isHeldWaypoint) heldWaypointPaints ?: fingerWaypointPaints else fingerWaypointPaints,
                    index,
                    waypoint.condition?.label,
                )
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
        labelLayouts.clear()
        animatedRatio = 0f
        gestureTotalDurationMs = 0L
    }

    private fun waypointPaints(fingerIndex: Int): WaypointPaints =
        if (fingerWaypointPaints.isEmpty()) defaultWaypointPaints
        else fingerWaypointPaints[fingerIndex % fingerWaypointPaints.size]

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

    private fun Canvas.drawConditionBitmap(position: PointF, bitmap: Bitmap) {
        conditionRect.set(
            position.x - bitmap.width / 2f,
            position.y - bitmap.height / 2f,
            position.x + bitmap.width / 2f,
            position.y + bitmap.height / 2f,
        )
        drawBitmap(bitmap, null, conditionRect, null)
    }

    private fun Canvas.drawSelectorCircle(
        position: PointF,
        backgroundPaint: Paint?,
        isEditable: Boolean,
        paints: WaypointPaints,
        fingerIndex: Int,
        label: String?,
    ) {
        val outerP = if (isEditable) paints.outer else paints.fixedOuter
        val innerP = if (isEditable) paints.inner else paints.fixedInner
        backgroundPaint?.let { drawCircle(position.x, position.y, viewStyle.outerRadiusPx * 2f, it) }

        val labelStyle = viewStyle.conditionLabelStyle
        if (label != null && labelStyle != null) {
            val labelBackgroundP = if (isEditable) paints.labelBackground else paints.fixedLabelBackground
            drawLabelledOuterCircle(position, label, labelStyle, outerP, labelBackgroundP, fingerIndex)
        } else {
            drawCircle(position.x, position.y, viewStyle.outerRadiusPx, outerP)
        }

        drawCircle(position.x, position.y, viewStyle.innerRadiusPx, innerP)
    }

    /**
     * Draws the outer circle thickened inward to contain [label], curved along it and centered at its bottom. The
     * label can go all around the circle and is ellipsized when it doesn't fit.
     */
    private fun Canvas.drawLabelledOuterCircle(
        position: PointF,
        label: String,
        labelStyle: GestureConditionLabelStyle,
        outerPaint: Paint,
        backgroundPaint: Paint,
        fingerIndex: Int,
    ) {
        val textPaint = labelStyle.textPaint
        val bandWidth = backgroundPaint.strokeWidth
        val outerEdgeRadius = viewStyle.outerRadiusPx + outerPaint.strokeWidth / 2f
        val arcRadius = outerEdgeRadius - bandWidth / 2f
        if (arcRadius <= 0f) {
            drawCircle(position.x, position.y, viewStyle.outerRadiusPx, outerPaint)
            return
        }

        drawCircle(position.x, position.y, arcRadius, backgroundPaint)

        val layout = labelLayouts.getOrPut(label) {
            // Keep a gap between the label end and start when it goes all around the circle
            val maxWidth = (2 * PI * arcRadius).toFloat() - bandWidth
            val text = TextUtils.ellipsize(label, textPaint, maxWidth, TextUtils.TruncateAt.END).toString()
            LabelLayout(text, Math.toDegrees((textPaint.measureText(text) / arcRadius).toDouble()).toFloat())
        }

        // Clockwise angles from the right; going counterclockwise along the bottom keeps the text upright.
        labelArcOval.set(position.x - arcRadius, position.y - arcRadius, position.x + arcRadius, position.y + arcRadius)
        labelArcPath.reset()
        labelArcPath.arcTo(labelArcOval, 90f + layout.sweepDegrees / 2f, -layout.sweepDegrees, true)

        val textColors = labelStyle.fingerTextColors
        val textColor = if (textColors.isEmpty()) Color.WHITE else textColors[fingerIndex % textColors.size]
        textPaint.color = textColor
        textPaint.alpha = Color.alpha(textColor) * outerPaint.alpha / 255
        drawTextOnPath(layout.text, labelArcPath, 0f, -(textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
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
    /** True if the finger stays pressed on its last waypoint once the gesture is completed. */
    val isHeld: Boolean = false,
)

data class GestureWaypoint(
    val position: PointF,
    val isEditable: Boolean = true,
    /** Identifier of the gesture motion this waypoint originates from, if any. Used to map an edition back to it. */
    val motionId: Identifier? = null,
    /** The condition this waypoint position is taken from, if any. */
    val condition: GestureWaypointCondition? = null,
)

/** How the condition a waypoint position is taken from is displayed. */
data class GestureWaypointCondition(
    /** Drawn at its size behind the waypoint, centered on it. */
    val bitmap: Bitmap? = null,
    /** Drawn at the bottom of the waypoint outer circle, ellipsized if too long. */
    val label: String? = null,
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
    /** Colour of each finger's waypoints, by finger index. When empty, the waypoint paints colours are kept. */
    @field:ColorInt val fingerColors: List<Int> = emptyList(),
    /** When non-null, colour of the last waypoint of a held finger instead of its finger colour. */
    @field:ColorInt val heldColor: Int? = null,
    /** When non-null, style of the [GestureWaypointCondition.label] drawn in waypoints. Labels are hidden otherwise. */
    val conditionLabelStyle: GestureConditionLabelStyle? = null,
)

data class GestureConditionLabelStyle(
    val textPaint: TextPaint,
    /** Colour of the label text, by finger index, to be readable on its finger colour. */
    @field:ColorInt val fingerTextColors: List<Int>,
    val paddingPx: Float,
)

internal fun Context.getGestureConditionLabelStyle(): GestureConditionLabelStyle =
    GestureConditionLabelStyle(
        textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = resources.getDimension(R.dimen.gesture_condition_label_text_size)
        },
        fingerTextColors = listOf(
            R.color.gesture_finger_1_label_text_color,
            R.color.gesture_finger_2_label_text_color,
            R.color.gesture_finger_3_label_text_color,
            R.color.gesture_finger_4_label_text_color,
            R.color.gesture_finger_5_label_text_color,
        ).map { ContextCompat.getColor(this, it) },
        paddingPx = resources.getDimension(R.dimen.gesture_condition_label_padding),
    )

internal fun Context.getGestureHeldColor(): Int =
    ContextCompat.getColor(this, R.color.gesture_finger_held_color)

internal fun Context.getGestureFingerColors(): List<Int> = listOf(
    R.color.gesture_finger_1_color,
    R.color.gesture_finger_2_color,
    R.color.gesture_finger_3_color,
    R.color.gesture_finger_4_color,
    R.color.gesture_finger_5_color,
).map { ContextCompat.getColor(this, it) }

private class WaypointPaints(
    val outer: Paint,
    val inner: Paint,
    val fixedOuter: Paint,
    val fixedInner: Paint,
    val labelBackground: Paint,
    val fixedLabelBackground: Paint,
) {
    fun withColor(@ColorInt newColor: Int): WaypointPaints = WaypointPaints(
        outer = outer.withColor(newColor),
        inner = inner.withColor(newColor),
        fixedOuter = fixedOuter.withColor(newColor),
        fixedInner = fixedInner.withColor(newColor),
        labelBackground = labelBackground.withColor(newColor),
        fixedLabelBackground = fixedLabelBackground.withColor(newColor),
    )
}

private class LabelLayout(val text: String, val sweepDegrees: Float)

private fun Paint.asLabelBand(widthPx: Float): Paint = Paint(this).apply {
    style = Paint.Style.STROKE
    strokeWidth = widthPx
}

private fun Paint.withColor(@ColorInt newColor: Int): Paint {
    val baseAlpha = alpha
    return Paint(this).apply {
        color = newColor
        alpha = Color.alpha(newColor) * baseAlpha / 255
    }
}

private const val MINIMAL_ANIMATION_DURATION_MS = 250L
