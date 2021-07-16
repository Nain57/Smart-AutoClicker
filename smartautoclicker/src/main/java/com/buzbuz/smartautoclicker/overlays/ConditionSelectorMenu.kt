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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.core.content.res.use
import androidx.core.graphics.toRect

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.scale
import com.buzbuz.smartautoclicker.extensions.move
import com.buzbuz.smartautoclicker.baseui.gestures.*
import com.buzbuz.smartautoclicker.baseui.overlays.OverlayMenuController
import com.buzbuz.smartautoclicker.model.DetectorModel

/**
 * [OverlayMenuController] implementation for displaying the area selection menu and the area to be captured in order
 * to create a new click condition.
 *
 * @param context the Android Context for the overlay menu shown by this controller.
 * @param onConditionSelected listener upon confirmation of the area to be capture to create the click condition.
 */
class ConditionSelectorMenu(
    context: Context,
    private val onConditionSelected: (Rect, Bitmap) -> Unit
) : OverlayMenuController(context) {

    private companion object {
        /** Delay before confirming the selection in order to let the time to the selector view to be hide. */
        private const val SELECTION_DELAY_MS = 200L

        /**  */
        @IntDef(SELECTION, CAPTURE, ADJUST)
        @Retention(AnnotationRetention.SOURCE)
        private annotation class ConditionCaptureState
        /**  */
        private const val SELECTION = 1
        /** */
        private const val CAPTURE = 2
        /**  */
        private const val ADJUST = 3
    }

    /** */
    private val selectorView = ConditionSelectorView(context)

    /** */
    @ConditionCaptureState
    private var state: Int = 0
        set(value) {
            field = value
            when (value) {
                SELECTION -> {
                    setMenuVisibility(View.VISIBLE)
                    selectorView.hide = true
                }
                CAPTURE -> {
                    setMenuVisibility(View.GONE)
                    selectorView.hide = true
                }
                ADJUST -> {
                    setMenuVisibility(View.VISIBLE)
                    selectorView.hide = false
                }
            }
        }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        layoutInflater.inflate(R.layout.overlay_validation_menu, null) as ViewGroup

    override fun onCreateOverlayView(): View = selectorView

    override fun onShow() {
        super.onShow()
        state = SELECTION
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> dismiss()
        }
    }

    /**  */
    private fun onConfirm() {
        if (state == SELECTION) {
            state = CAPTURE
            Handler(Looper.getMainLooper()).postDelayed({
                DetectorModel.get().captureScreenArea(Rect(selectorView.maxArea.toRect())) { bitmap ->
                    selectorView.apply {
                        screenCapture = BitmapDrawable(resources, bitmap)
                    }
                    state = ADJUST
                }
            }, SELECTION_DELAY_MS)
        } else {
            val selectedArea = Rect(selectorView.selectedArea.toRect())
            val crop = Bitmap.createBitmap(selectorView.screenCapture!!.bitmap, selectedArea.left, selectedArea.top,
                selectedArea.width(), selectedArea.height())
            onConditionSelected(selectedArea, crop)
            dismiss()
        }
    }

    /** Overlay view used as [screenOverlayView] showing the area to capture the content as a click condition. */
    private inner class ConditionSelectorView(context: Context) : View(context) {

        /** The list of gestures applied to this view. */
        private val gestures: List<Gesture>
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

        /** */
        var screenCapture: BitmapDrawable? = null
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

            gestures = listOf(
                MoveGesture(this, resizeHandleSize, ::moveTo),
                ResizeLeftGesture(this, resizeHandleSize, ::resize),
                ResizeTopGesture(this, resizeHandleSize, ::resize),
                ResizeRightGesture(this, resizeHandleSize, ::resize),
                ResizeBottomGesture(this, resizeHandleSize, ::resize),
                ScaleGesture(this, resizeHandleSize, context, ::scale)
            )

            hintsIcons = HintsController(context, hintIconsSize, maxArea, hintIconsMargin, outlineColor,
                hintFadeDuration.toLong(), hintAllFadeDelay.toLong(), this)
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
            gestures.forEach { gesture ->
                if (gesture.onTouchEvent(event, selectorArea)) {
                    return true
                }
            }
            return false
        }

        /**
         * Called when a [Gesture] detects a scale gesture.
         * Apply the scale factor to the selector and invalidate the view to redraw it.
         *
         * @param factor the scale factor detected.
         */
        private fun scale(factor: Float) {
            selectorArea.scale(factor)
            invalidate()
        }

        /**
         * Called when a [Gesture] detects a move gesture.
         * Apply the translation parameters to the selector and invalidate the view to redraw it.
         *
         * @param toCenterX the new x position.
         * @param toCenterY the new y position.
         */
        private fun moveTo(toCenterX: Float, toCenterY: Float) {
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
        private fun resize(newSize: RectF, @GestureType type: Int) {
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
                setBounds(0, 0, maxArea.width().toInt(), maxArea.height().toInt())
                draw(canvas)
            }

            canvas.drawRoundRect(selectorArea, cornerRadius, cornerRadius, selectorPaint)
            canvas.drawRect(selectedArea, backgroundPaint)
            hintsIcons.onDraw(canvas)
        }
    }
}
