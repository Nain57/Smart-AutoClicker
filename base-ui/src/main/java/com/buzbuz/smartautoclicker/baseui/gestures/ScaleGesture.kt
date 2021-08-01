/*
 * Copyright (C) 2020 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.baseui.gestures

import android.content.Context
import android.graphics.RectF
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

import androidx.annotation.VisibleForTesting

/**
 * Gesture wrapping a [ScaleGestureDetector] for scaling the view with a pinch.
 *
 * @param view the view to perform the gesture on.
 * @param handleSize the minimum size of the area where the user can interact with a gesture.
 * @param onScaleListener the object to notify upon scale gesture results.
 * Move listener parameter is the scale factor to apply to the view.
 */
class ScaleGesture(
    view: View,
    handleSize: Float,
    context: Context,
    vibrate: Boolean,
    private val onScaleListener: (Float) -> Unit
) : Gesture(view, handleSize, vibrate, true) {

    /** Called when the scale gesture is detected and propagate the scale factor to the listener. */
    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            onScaleListener(detector.scaleFactor)
            return true
        }
    }
    /** The Android scale gesture detector handling the touch events to detect the pinch for scaling. */
    private val scaleDetector: ScaleGestureDetector by lazy {
        scaleDetectorSupplier?.invoke(scaleListener) ?: ScaleGestureDetector(context, scaleListener)
    }
    /** Test member allowing to inject a [ScaleGestureDetector]. */
    @VisibleForTesting
    internal var scaleDetectorSupplier: ((ScaleGestureDetector.SimpleOnScaleGestureListener) -> ScaleGestureDetector)? = null

    override val gestureType = SCALE
    override fun onDownEvent(event: MotionEvent, viewArea: RectF): Boolean = scaleDetector.onTouchEvent(event)
    override fun onEvent(event: MotionEvent, viewArea: RectF): Boolean = scaleDetector.onTouchEvent(event)
    override fun onGesturePointerUp(event: MotionEvent, viewArea: RectF): Boolean = scaleDetector.onTouchEvent(event)
}