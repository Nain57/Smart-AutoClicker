/*
 * Copyright (C) 2024 Kevin Buzeau
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