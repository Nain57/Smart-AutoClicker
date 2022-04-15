/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.baseui.menu.overlayviews.condition

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.KeyEvent.ACTION_UP
import android.view.MotionEvent
import android.view.View

import androidx.core.content.res.use
import androidx.core.graphics.minus

import com.buzbuz.smartautoclicker.baseui.menu.overlayviews.condition.selector.Selector
import com.buzbuz.smartautoclicker.baseui.menu.overlayviews.condition.selector.HintsController
import com.buzbuz.smartautoclicker.baseui.ScreenMetrics
import com.buzbuz.smartautoclicker.ui.R

/**
 * Overlay view used as screenOverlayView showing the area to capture the content as an event condition.
 * This view allows to zoom/move the bitmap displayed as background, as well as display a selector over it allowing to
 * easily select a section of the screen for a event condition.
 *
 * @param context the Android context
 * @param screenMetrics the current screen metrics.
 * @param onSelectorValidityChanged listener upon the selector validity.
 */
@SuppressLint("ViewConstructor") // Not intended to be used from XML
class ConditionSelectorView(
    context: Context,
    private val screenMetrics: ScreenMetrics,
    private val onSelectorValidityChanged: (Boolean) -> Unit,
) : View(context) {

    /** Controls the display of the bitmap captured. */
    private lateinit var capture: Capture
    /** Controls the display of the selector. */
    private lateinit var selector: Selector
    /** Controls the display of the user hints around the selector. */
    private lateinit var hintsIcons: HintsController
    /** Controls the animations. */
    private lateinit var animations: Animations

    /** Tells if the selector is at a valid position relatively to the capture position. */
    private var isSelectorValid = false
    /** Used during selector validation. kept here to avoid instantiation at each touch event. */
    private val selectorValidityTempValue = RectF()

    /** Get the attributes from the style file and initialize all components. */
    init {
        context.obtainStyledAttributes(R.style.OverlaySelectorView_Condition, R.styleable.ConditionSelectorView).use { ta ->
            animations = Animations(ta)
            capture = Capture(context, ta, screenMetrics, ::invalidate)
            selector = Selector(context, ta, screenMetrics, ::invalidate)
            hintsIcons = HintsController(context, ta, screenMetrics, ::invalidate)
        }
    }

    /** Setup the position changes callbacks. */
    init {
        selector.onSelectorPositionChanged = { position ->
            hintsIcons.setSelectorArea(position)
            verifySelectorValidity()
        }
        capture.onCapturePositionChanged = { _ ->
            verifySelectorValidity()
        }
    }

    /** Setup animation values callback. */
    init {
        animations.apply {
            onCaptureZoomLevelChanged = { zoomLevel ->
                capture.setZoomLevel(zoomLevel)
            }
            onSelectorBorderAlphaChanged = { alpha ->
                selector.selectorAlpha = alpha
            }
            onSelectorBackgroundAlphaChanged = { alpha ->
                selector.backgroundAlpha = alpha
            }
            onHintsAlphaChanged = { alpha ->
                hintsIcons.alpha = alpha
            }
        }
    }

    /** Tell if the content of this view should be hidden or not. */
    var hide = true
        set(value) {
            if (field == value) {
                return
            }
            field = value

            if (value) {
                capture.onReset()
                selector.onReset()
                hintsIcons.onReset()
            }
            invalidate()
        }

    /**
     * Verifies if the [selector] is at a valid position with the [capture].
     * If the validation position value changes, notifies [onSelectorValidityChanged].
     */
    private fun verifySelectorValidity() {
        selectorValidityTempValue.set(RectF(selector.selectedArea))
        if (selectorValidityTempValue.intersect(capture.captureArea.minus(CAPTURE_MINIMUM_SIZE)) != isSelectorValid) {
            isSelectorValid = !isSelectorValid
            onSelectorValidityChanged(isSelectorValid)
        }
    }

    /**
     * Shows the capture on the screen.
     *
     * @param bitmap the capture the be shown.
     */
    fun showCapture(bitmap: Bitmap) {
        capture.screenCapture = BitmapDrawable(resources, bitmap)
        hintsIcons.showAll()
        animations.startShowSelectorAnimation(
            onAnimationCompleted = {
                animations.startHideHintsAnimation()
            }
        )
    }

    /**
     * Get the part of the capture that is currently selected within the selector.
     *
     * @return a pair of the capture area and a bitmap of its content.
     */
    fun getSelection(): Pair<Rect, Bitmap> {
        if (!isSelectorValid) {
            throw IllegalStateException("Can't get a selection, selector is invalid.")
        }

        val selectionArea = selector.getSelectionArea(capture.captureArea, capture.zoomLevel)
        return selectionArea to Bitmap.createBitmap(capture.screenCapture!!.bitmap, selectionArea.left,
            selectionArea.top, selectionArea.width(), selectionArea.height())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        selector.onViewSizeChanged(w, h)
        capture.onViewSizeChanged(w, h)
        hintsIcons.onViewSizeChanged(w, h)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || animations.isShowSelectorAnimationRunning()) {
            return false
        }

        selector.currentGesture?.let { gestureType ->
            hintsIcons.show(gestureType)
            animations.cancelHideHintsAnimation()

            if (event.action == ACTION_UP) {
                animations.startHideHintsAnimation()
            }
        }

        return selector.onTouchEvent(event) || capture.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas) {
        if (hide) {
            return
        }

        capture.onDraw(canvas)
        selector.onDraw(canvas)
        hintsIcons.onDraw(canvas)
    }
}

/**
 * The minimum size of the capture.
 * Final results will not always be the size, as it is relative to the capture viewport, but we just don't want a
 * null result.
 */
private const val CAPTURE_MINIMUM_SIZE = 50f