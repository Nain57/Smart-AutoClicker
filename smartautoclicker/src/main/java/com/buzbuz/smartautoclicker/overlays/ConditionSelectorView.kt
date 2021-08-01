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
package com.buzbuz.smartautoclicker.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.res.use
import androidx.core.graphics.toRect
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.gestures.*
import com.buzbuz.smartautoclicker.extensions.ScreenMetrics
import com.buzbuz.smartautoclicker.extensions.move

/**
 * Overlay view used as screenOverlayView showing the area to capture the content as a click condition.
 * This view allows to zoom/move the bitmap displayed as background, as well as display a selector over it allowing to
 * easily select a section of the screen for a click condition.
 *
 * @param context the Android context
 * @param screenMetrics the current screen metrics.
 */
@SuppressLint("ViewConstructor") // Not intended to be used from XML
class ConditionSelectorView(
    context: Context,
    private val screenMetrics: ScreenMetrics
) : View(context) {

    private companion object {
        /** The minimum zoom value. */
        private const val ZOOM_MINIMUM = 0.8f
        /** The maximum zoom value. */
        private const val ZOOM_MAXIMUM = 3f
    }

    /** The list of gestures applied to the selector. */
    private val selectorGestures: List<Gesture>
    /** The list of gestures applied to the capture. */
    private val captureGestures: List<Gesture>
    /** Paint drawing the selector. */
    private val selectorPaint = Paint()
    /** Paint for the background of the selector. */
    private val backgroundPaint = Paint()
    /** Controls the display of the user hints around the selector. */
    private val hintsIcons: HintsController

    /** The radius of the corner for the selector. */
    private var cornerRadius = 0f
    /** Default width of the selector area. */
    private var defaultWidth = 100f
    /** Default height of the selector area. */
    private var defaultHeight = 100f
    /** The area where the selector should be drawn. */
    private var selectorArea = RectF()
    /** Difference between the center of the selector and its inner content. */
    private var selectorAreaOffset = 0

    /** The current area where the capture is displayed. It can be bigger than the screen when zoomed. */
    private val captureArea = RectF()
    /** The drawable for the screen capture. */
    private var screenCapture: BitmapDrawable? = null
    /** The current center position of the capture. It can be outside of the screen when zoomed. */
    private var captureCenter = PointF()
    /** The current zoom level*/
    private var zoomLevel = 1f
        set(value) {
            field = value.coerceIn(ZOOM_MINIMUM, ZOOM_MAXIMUM)
        }

    /** The maximum size of the selector. */
    val maxArea: RectF
    /** Area within the selector that represents the zone to be capture to creates a click condition. */
    var selectedArea = RectF()
    /** Tell if the content of this view should be hidden or not. */
    var hide = true
        set(value) {
            if (field == value) {
                return
            }

            field = value
            if (value) {
                hintsIcons.showAll()
            }
            invalidate()
        }

    init {
        val screenSize = screenMetrics.getScreenSize()
        maxArea = RectF(0f, 0f, screenSize.x.toFloat(), screenSize.y.toFloat())
        captureCenter.x = maxArea.centerX()
        captureCenter.y = maxArea.centerY()
    }

    init {
        var hintIconsSize = 10
        var hintIconsMargin = 5
        @ColorInt var outlineColor = Color.WHITE
        var hintFadeDuration = 500
        var hintAllFadeDelay = 1000
        var resizeHandleSize = 10f

        context.obtainStyledAttributes(R.style.OverlaySelectorView_Condition, R.styleable.ConditionSelectorView).use { ta ->
            hintIconsSize = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_hintsIconsSize, hintIconsSize)
            hintIconsMargin = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_hintsIconsMargin, hintIconsMargin)
            outlineColor =  ta.getColor(R.styleable.ConditionSelectorView_colorOutlinePrimary, outlineColor)
            hintFadeDuration = ta.getInteger(R.styleable.ConditionSelectorView_hintsFadeDuration, hintFadeDuration)
            hintAllFadeDelay = ta.getInteger(R.styleable.ConditionSelectorView_hintsAllFadeDelay, hintAllFadeDelay)

            cornerRadius = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_cornerRadius, 2)
                .toFloat()
            defaultWidth = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_defaultWidth, 100)
                .toFloat() / 2
            defaultHeight = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_defaultHeight, 100)
                .toFloat() / 2
            selectorArea = RectF(maxArea.centerX() - defaultWidth, maxArea.centerY() - defaultHeight,
                maxArea.centerX() + defaultWidth, maxArea.centerY() + defaultHeight)

            val thickness = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_thickness, 4).toFloat()
            selectorAreaOffset = kotlin.math.ceil(thickness / 2).toInt()
            selectorPaint.apply {
                style = Paint.Style.STROKE
                strokeWidth = thickness
                color = outlineColor
            }
            backgroundPaint.apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = ta.getColor(R.styleable.ConditionSelectorView_colorBackground, Color.TRANSPARENT)
            }

            resizeHandleSize = ta
                .getDimensionPixelSize(R.styleable.ConditionSelectorView_resizeHandleSize, 10)
                .toFloat()
        }

        selectorGestures = listOf(
            MoveGesture(this, resizeHandleSize, true, ::moveSelectorTo),
            ResizeLeftGesture(this, resizeHandleSize, true,  ::resizeSelector),
            ResizeTopGesture(this, resizeHandleSize, true, ::resizeSelector),
            ResizeRightGesture(this, resizeHandleSize, true, ::resizeSelector),
            ResizeBottomGesture(this, resizeHandleSize, true, ::resizeSelector)
        )

        captureGestures = listOf(
            MoveGesture(this, resizeHandleSize, false, ::moveCaptureTo),
            ScaleGesture(this, resizeHandleSize, context, false, ::scaleCapture)
        )

        hintsIcons = HintsController(context, hintIconsSize, maxArea, hintIconsMargin, outlineColor,
            hintFadeDuration.toLong(), hintAllFadeDelay.toLong(), this)

        updateCapturePosition()
    }

    /**
     * Shows the capture on the screen.
     *
     * @param bitmap the capture the be shown.
     */
    fun showCapture(bitmap: Bitmap) {
        screenCapture = BitmapDrawable(resources, bitmap)
        scaleCapture(0.8f)
    }

    /**
     * Get the part of the capture that is currently selected within the selector.
     *
     * @return a pair of the capture area and a bitmap of its content.
     */
    fun getSelection(): Pair<Rect, Bitmap> {
        val selectedArea = RectF(selectedArea).run {
            val invertedZoomLevel = 1 / zoomLevel

            left = (left - captureArea.left) * invertedZoomLevel
            top = (top - captureArea.top) * invertedZoomLevel
            right = (right - captureArea.left) * invertedZoomLevel
            bottom = (bottom - captureArea.top) * invertedZoomLevel

            toRect()
        }

        return selectedArea to Bitmap.createBitmap(screenCapture!!.bitmap, selectedArea.left,
            selectedArea.top, selectedArea.width(), selectedArea.height())
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val screenSize = screenMetrics.getScreenSize()
        maxArea.apply {
            right = screenSize.x.toFloat()
            bottom = screenSize.y.toFloat()
        }
        selectorArea.apply {
            left = maxArea.centerX() - defaultWidth
            top = maxArea.centerY() - defaultHeight
            right = maxArea.centerX() + defaultWidth
            bottom = maxArea.centerY() + defaultHeight
        }
        hintsIcons.showAll()
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        selectorGestures.forEach { gesture ->
            if (gesture.onTouchEvent(event, selectorArea)) {
                return true
            }
        }
        captureGestures.forEach { gesture ->
            if (gesture.onTouchEvent(event, captureArea)) {
                return true
            }
        }

        return false
    }

    /**
     * Called when a [Gesture] detects a move gesture.
     * Apply the translation parameters to the selector and invalidate the view to redraw it.
     *
     * @param toCenterX the new x position.
     * @param toCenterY the new y position.
     */
    private fun moveCaptureTo(toCenterX: Float, toCenterY: Float) {
        captureCenter.x = toCenterX
        captureCenter.y = toCenterY
        updateCapturePosition()
    }

    /**
     * Called when a [Gesture] detects a scale gesture.
     * Apply the scale factor to the selector and invalidate the view to redraw it.
     *
     * @param factor the scale factor detected.
     */
    private fun scaleCapture(factor: Float) {
        zoomLevel *= factor
        updateCapturePosition()
    }

    /** Update the position of the captured bitmap on the screen and invalidate. */
    private fun updateCapturePosition() {
        val newHalfWidth = maxArea.width() * zoomLevel / 2
        val newHalfHeight = maxArea.height() * zoomLevel / 2
        captureArea.apply {
            left = captureCenter.x - newHalfWidth
            top = captureCenter.y - newHalfHeight
            right = captureCenter.x + newHalfWidth
            bottom = captureCenter.y + newHalfHeight
        }
        invalidate()
    }

    /**
     * Called when a [Gesture] detects a move gesture.
     * Apply the translation parameters to the selector and invalidate the view to redraw it.
     *
     * @param toCenterX the new x position.
     * @param toCenterY the new y position.
     */
    private fun moveSelectorTo(toCenterX: Float, toCenterY: Float) {
        val halfWidth = selectedArea.width() / 2
        val halfHeight = selectedArea.height() / 2
        val xPos = if (toCenterX in maxArea.left + halfWidth .. maxArea.right - halfWidth) toCenterX else selectorArea.centerX()
        val yPos = if (toCenterY in maxArea.top + halfHeight .. maxArea.bottom - halfHeight) toCenterY else selectorArea.centerY()

        selectorArea.move(xPos, yPos)
        hintsIcons.show(MOVE)
        invalidate()
    }

    /**
     * Called when a [Gesture] detects a resize gesture.
     * Apply the new size to the selector and invalidate the view to redraw it.
     *
     * @param newSize the new area of the selector after the resize.
     */
    private fun resizeSelector(newSize: RectF, @GestureType type: Int) {
        selectorArea = newSize
        hintsIcons.show(type)
        invalidate()
    }

    override fun invalidate() {
        selectorArea.intersect(maxArea)
        selectedArea.apply {
            left = selectorArea.left + selectorAreaOffset
            top = selectorArea.top + selectorAreaOffset
            right = selectorArea.right - selectorAreaOffset
            bottom = selectorArea.bottom - selectorAreaOffset
        }
        hintsIcons.invalidate(selectorArea.toRect())

        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (hide) {
            return
        }

        screenCapture?.apply {
            canvas.drawColor(Color.BLACK)
            setBounds(
                captureArea.left.toInt(),
                captureArea.top.toInt(),
                captureArea.right.toInt(),
                captureArea.bottom.toInt()
            )
            draw(canvas)
        }

        canvas.drawRoundRect(selectorArea, cornerRadius, cornerRadius, selectorPaint)
        canvas.drawRect(selectedArea, backgroundPaint)
        hintsIcons.onDraw(canvas)
    }
}