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
package com.buzbuz.smartautoclicker.ui.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.use
import androidx.core.graphics.toRect

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.displaySize
import com.buzbuz.smartautoclicker.extensions.scale
import com.buzbuz.smartautoclicker.extensions.translate
import com.buzbuz.smartautoclicker.ui.base.OverlayMenuController

/**
 * [OverlayMenuController] implementation for displaying the area selection menu and the area to be captured in order
 * to create a new click condition.
 *
 * @param context the Android Context for the overlay menu shown by this controller.
 * @param onConditionSelected listener upon confirmation of the area to be capture to create the click condition.
 */
class ConditionSelectorMenu(
    context: Context,
    private val onConditionSelected: (Rect) -> Unit
) : OverlayMenuController(context) {

    override val menuLayoutRes: Int = R.layout.overlay_validation_menu
    override val screenOverlayView: View? = ConditionSelectorView(context)

    override fun onItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> dismiss()
        }
    }

    /** Confirm the current condition selection, notify the listener and dismiss the overlay. */
    private fun onConfirm() {
        onConditionSelected.invoke((screenOverlayView as ConditionSelectorView).selectedArea.toRect())
        dismiss()
    }

    /** Overlay view used as [screenOverlayView] showing the area to capture the content as a click condition. */
    private inner class ConditionSelectorView(context: Context) : View(context) {

        /** Compute the touch events to detect the resize/scale/move gesture to apply to the selector drawn in this view */
        private val resizeDetector = ResizeGestureDetector(context, this, ::scale, ::moveTo, ::resize)
        /** The maximum size of the selector. */
        private val maxArea: RectF
        /** Paint drawing the selector. */
        private val selectorPaint = Paint()
        /** Paint for the background of the selector. */
        private val backgroundPaint = Paint()

        /** The radius of the corner for the selector. */
        private var cornerRadius = 0f
        /** The area where the selector should be drawn. */
        private var selectorArea = RectF()
        /** Difference between the center of the selector and its inner content. */
        private var selectorAreaOffset = 0

        /** Area within the selector that represents the zone to be capture to creates a click condition. */
        var selectedArea = RectF()

        init {
            val screenSize = windowManager.displaySize
            maxArea = RectF(0f, 0f, screenSize.x.toFloat(), screenSize.y.toFloat())
        }

        init {
            context.obtainStyledAttributes(R.style.OverlaySelectorView_Condition, R.styleable.ConditionSelectorView).use { ta ->
                cornerRadius = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_cornerRadius, 2)
                    .toFloat()
                val xOffset = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_defaultWidth, 100)
                    .toFloat() / 2
                val yOffset = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_defaultHeight, 100)
                    .toFloat() / 2
                selectorArea = RectF(maxArea.centerX() - xOffset, maxArea.centerY() - yOffset,
                    maxArea.centerX() + xOffset, maxArea.centerY() + yOffset)

                val thickness = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_thickness, 4).toFloat()
                selectorAreaOffset = kotlin.math.ceil(thickness / 2).toInt()
                selectorPaint.apply {
                    style = Paint.Style.STROKE
                    strokeWidth = thickness
                    color = ta.getColor(R.styleable.ConditionSelectorView_colorOutlinePrimary, Color.RED)
                }
                backgroundPaint.apply {
                    isAntiAlias = true
                    style = Paint.Style.FILL
                    color = ta.getColor(R.styleable.ConditionSelectorView_colorBackground, Color.TRANSPARENT)
                }

                resizeDetector.resizeHandleSize = ta
                    .getDimensionPixelSize(R.styleable.ConditionSelectorView_resizeHandleSize, 10)
                    .toFloat()
            }

            invalidate()
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            return resizeDetector.onTouchEvent(event, selectorArea)
        }

        /**
         * Called when the [resizeDetector] detects a scale gesture.
         * Apply the scale factor to the selector and invalidate the view to redraw it.
         *
         * @param factor the scale factor detected.
         */
        private fun scale(factor: Float) {
            selectorArea.scale(factor)
            invalidate()
        }

        /**
         * Called when the [resizeDetector] detects a move gesture.
         * Apply the translation parameters to the selector and invalidate the view to redraw it.
         *
         * @param toX the new x position.
         * @param toY the new y position.
         */
        private fun moveTo(toX: Float, toY: Float) {
            val xPos = when {
                toX < maxArea.left -> maxArea.left
                toX + selectorArea.width() > maxArea.right -> maxArea.right - selectorArea.width()
                else -> toX
            }
            val yPos = when {
                toY < maxArea.top -> maxArea.top
                toY + selectorArea.height() > maxArea.bottom -> maxArea.bottom - selectorArea.height()
                else -> toY
            }
            selectorArea.translate(xPos, yPos)
            invalidate()
        }

        /**
         * Called when the [resizeDetector] detects a resize gesture.
         * Apply the new size to the selector and invalidate the view to redraw it.
         *
         * @param newSize the new area of the selector after the resize.
         */
        private fun resize(newSize: RectF) {
            selectorArea = newSize
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

            super.invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawRoundRect(selectorArea, cornerRadius, cornerRadius, selectorPaint)
            canvas.drawRect(selectedArea, backgroundPaint)
        }
    }
}
