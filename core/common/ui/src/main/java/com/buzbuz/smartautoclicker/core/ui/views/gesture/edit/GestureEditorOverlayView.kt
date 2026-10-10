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
package com.buzbuz.smartautoclicker.core.ui.views.gesture.edit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureBriefRenderer
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureBriefRendererStyle
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureFingerPath
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.getGestureConditionLabelStyle
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.getGestureFingerColors
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.getGestureHeldColor

import kotlin.math.roundToInt
import kotlin.math.sqrt


@SuppressLint("ViewConstructor")
class GestureEditorOverlayView(context: Context) : View(context) {

    /** The waypoint currently dragged, identified by its finger index and motion id. */
    private data class DragState(
        val fingerIndex: Int,
        val motionId: Identifier,
        val currentPos: PointF,
    )

    private val density = context.resources.displayMetrics.density
    private val outerRadius = 28f * density
    private val innerRadius = 3f * density
    private val touchRadius = 36f * density

    private val gestureRenderer = GestureBriefRenderer(
        briefView = this,
        viewStyle = GestureBriefRendererStyle(
            backgroundColor = EDITOR_GLOW_COLOR,
            fixedBackgroundColor = EDITOR_FIXED_GLOW_COLOR,
            outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 6f * density
            },
            innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            },
            fixedOuterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 110
                style = Paint.Style.STROKE
                strokeWidth = 6f * density
            },
            fixedInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 110
                style = Paint.Style.FILL
            },
            linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 200
                strokeWidth = 3f * density
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            },
            outerRadiusPx = outerRadius,
            innerRadiusPx = innerRadius,
            travelerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            },
            fingerColors = context.getGestureFingerColors(),
            heldColor = context.getGestureHeldColor(),
            conditionLabelStyle = context.getGestureConditionLabelStyle(),
        )
    )

    /** The gesture displayed and edited by this view. */
    var gesture: GestureDescription? = null
        set(value) {
            field = value
            // For SingleMotion: derive displayedFingerIndex from the first gesture that arrives
            if (targetMotionId != null && displayedFingerIndex < 0 && value != null) {
                displayedFingerIndex = value.fingers.indexOfFirst { finger ->
                    finger.waypoints.any { wp -> wp.isEditable && wp.motionId == targetMotionId }
                }
            }

            if (!isActivelyDragging) {
                dragState = null
                updateRenderer()
            }

            invalidate()
        }

    /**
     * When non-null, replaces the finger at [displayedFingerIndex] in all rendering.
     * Used during recording to show the live-captured path in place of the stored one.
     */
    var recordingFingerPath: GestureFingerPath? = null
        set(value) {
            field = value
            updateRenderer()
            invalidate()
        }

    /** Called when a waypoint drag completes (ACTION_UP). Null in single-motion mode. */
    var onPositionDragEnded: ((fingerIndex: Int, motionId: Identifier, position: Point) -> Unit)? = null

    var isSingleMotionMode: Boolean = false

    /**
     * In SingleMotion mode, the motionId whose finger should be isolated in the display.
     * When non-null, [displayedFingerIndex] is derived from the first [gesture] emission.
     */
    var targetMotionId: Identifier? = null

    /** When >= 0, only this finger index is rendered and interactable. */
    var displayedFingerIndex: Int = -1

    private var dragState: DragState? = null
    private var isActivelyDragging = false

    /** Returns the last dragged position for the given motionId, or the current display position. */
    fun getCurrentPosition(motionId: Identifier): Point? {
        val ds = dragState
        if (ds != null && ds.motionId == motionId) {
            return Point(ds.currentPos.x.roundToInt(), ds.currentPos.y.roundToInt())
        }

        gesture?.fingers?.forEach { finger ->
            finger.waypoints.forEach { wp ->
                if (wp.isEditable && wp.motionId == motionId) {
                    return Point(wp.position.x.roundToInt(), wp.position.y.roundToInt())
                }
            }
        }
        return null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gestureRenderer.onStop()
    }

    /** Ensures gradient paints are rebuilt with current waypoint positions before each draw. */
    override fun invalidate() {
        gestureRenderer.onInvalidate()
        super.invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean =
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val target = if (isSingleMotionMode) findFirstEditable()
                    else findNearestEditable(event.x, event.y)
                target ?: return false

                isActivelyDragging = true
                gestureRenderer.isActivelyDragging = true
                dragState =
                    if (isSingleMotionMode) target.copy(currentPos = PointF(event.x, event.y))
                    else target

                invalidate()
                true
            }

            MotionEvent.ACTION_MOVE -> {
                val ds = dragState ?: return false

                dragState = ds.copy(currentPos = PointF(event.x, event.y))
                gestureRenderer.updateFingerPaths(renderedFingerPaths(dragState))

                invalidate()
                true
            }

            MotionEvent.ACTION_UP -> {
                val ds = dragState ?: return false
                val finalPos = PointF(event.x, event.y)
                isActivelyDragging = false
                gestureRenderer.isActivelyDragging = false
                dragState = ds.copy(currentPos = finalPos)

                onPositionDragEnded?.invoke(
                    ds.fingerIndex,
                    ds.motionId,
                    Point(finalPos.x.roundToInt(), finalPos.y.roundToInt()),
                )

                invalidate()
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                isActivelyDragging = false
                gestureRenderer.isActivelyDragging = false
                dragState = null

                invalidate()
                true
            }
            else -> false
        }

    override fun onDraw(canvas: Canvas) {
        gestureRenderer.onDraw(canvas)
    }

    private fun updateRenderer() {
        val description = gesture
        if (description == null) {
            gestureRenderer.onStop()
            return
        }

        gestureRenderer.onNewDescription(
            description = description.copy(fingers = renderedFingerPaths(dragState = null)),
            animate = true,
        )
    }

    private fun findFirstEditable(): DragState? {
        gesture?.fingers?.forEachIndexed { fingerIndex, finger ->
            if (displayedFingerIndex >= 0 && fingerIndex != displayedFingerIndex) return@forEachIndexed

            finger.waypoints.forEach { wp ->
                val motionId = wp.motionId
                if (wp.isEditable && motionId != null) return DragState(
                    fingerIndex = fingerIndex,
                    motionId = motionId,
                    currentPos = PointF(wp.position.x, wp.position.y),
                )
            }
        }
        return null
    }

    private fun findNearestEditable(x: Float, y: Float): DragState? {
        var nearest: DragState? = null
        var nearestDist = touchRadius

        gesture?.fingers?.forEachIndexed { fingerIndex, finger ->
            if (displayedFingerIndex >= 0 && fingerIndex != displayedFingerIndex) return@forEachIndexed
            finger.waypoints.forEach { wp ->
                val motionId = wp.motionId
                if (!wp.isEditable || motionId == null) return@forEach

                val dx = wp.position.x - x
                val dy = wp.position.y - y
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist < nearestDist) {
                    nearestDist = dist
                    nearest = DragState(
                        fingerIndex = fingerIndex,
                        motionId = motionId,
                        currentPos = PointF(wp.position.x, wp.position.y),
                    )
                }
            }
        }

        return nearest
    }

    private fun renderedFingerPaths(dragState: DragState?): List<GestureFingerPath> {
        val fingers = gesture?.fingers ?: return emptyList()

        if (displayedFingerIndex >= 0) {
            val finger = fingers.getOrNull(displayedFingerIndex) ?: return emptyList()
            return listOf(recordingFingerPath ?: finger.withDraggedWaypoint(dragState))
        }

        return fingers.map { finger -> finger.withDraggedWaypoint(dragState) }
    }

    private fun GestureFingerPath.withDraggedWaypoint(dragState: DragState?): GestureFingerPath {
        dragState ?: return this

        return copy(
            waypoints = waypoints.map { wp ->
                if (wp.isEditable && wp.motionId == dragState.motionId) wp.copy(position = dragState.currentPos, condition = null)
                else wp
            },
        )
    }
}

/** Glow gradient colour for editable waypoints — matches @color/overlayActionsBriefBackground. */
private val EDITOR_GLOW_COLOR = Color.argb(0x99, 0, 0, 0)

/** Dimmer glow gradient colour for fixed (non-editable) waypoints. */
private val EDITOR_FIXED_GLOW_COLOR = Color.argb(66, 0, 0, 0)
