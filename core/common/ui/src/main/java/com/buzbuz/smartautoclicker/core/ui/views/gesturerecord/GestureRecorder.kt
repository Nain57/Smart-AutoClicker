
package com.buzbuz.smartautoclicker.core.ui.views.gesturerecord

import android.graphics.PointF
import android.view.MotionEvent
import kotlin.math.pow
import kotlin.math.sqrt


internal class GestureRecorder(
    private val onNewCapturedGesture: (gesture: RecordedGesture?, isFinished: Boolean) -> Unit,
) {

    private var originEventPosition: PointF? = null

    fun processEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) {
            clearCapture()
            return true
        }

        if (originEventPosition == null) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                startCapture(event)
                return true
            }

            return false
        }

        when (event.action) {
            MotionEvent.ACTION_MOVE -> updateCapture(event)
            MotionEvent.ACTION_UP -> endCapture(event)
            MotionEvent.ACTION_CANCEL -> clearCapture()
            else -> return false
        }

        return true
    }

    fun clearCapture() {
        originEventPosition = null
    }

    private fun startCapture(event: MotionEvent) {
        originEventPosition = PointF(event.x, event.y)

        // Always start with a click
        onNewCapturedGesture(RecordedGesture.Click(originEventPosition!!, 1L), false)
    }

    private fun updateCapture(event: MotionEvent) {
        val origin = originEventPosition ?: return
        onNewCapturedGesture(origin.getGestureWith(event), false)
    }

    private fun endCapture(event: MotionEvent) {
        val origin = originEventPosition ?: return
        originEventPosition = null
        onNewCapturedGesture(origin.getGestureWith(event), true)
    }

    private fun PointF.getGestureWith(lastEvent: MotionEvent): RecordedGesture =
        if (willBeAClick(lastEvent)) {
            RecordedGesture.Click(
                position = this,
                durationMs = lastEvent.eventTime - lastEvent.downTime,
            )
        } else {
            RecordedGesture.Swipe(
                from = this,
                to = PointF(lastEvent.x, lastEvent.y),
                durationMs = lastEvent.eventTime - lastEvent.downTime,
            )
        }

    private fun PointF.willBeAClick(lastEvent: MotionEvent): Boolean {
        return sqrt((x - lastEvent.x).pow(2) + (y - lastEvent.y).pow(2)) <= SWIPE_MIN_DISTANCE_PX
    }
}

private const val SWIPE_MIN_DISTANCE_PX = 40f