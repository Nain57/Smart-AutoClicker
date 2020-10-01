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
        onConditionSelected.invoke((screenOverlayView as ConditionSelectorView).selectedArea)
        dismiss()
    }

    /** Overlay view used as [screenOverlayView] showing the area to capture the content as a click condition. */
    private inner class ConditionSelectorView(context: Context) : View(context) {

        /** Compute the touch events to detect the resize/scale/move gesture to apply to the selector drawn in this view */
        private val resizeDetector = ResizeGestureDetector(context, this, ::scale, ::moveTo, ::resize)
        /** The maximum size of the selector. */
        private val maxArea: RectF
        /** Paint for drawing the selector on the Canvas. */
        private val paint = Paint()
        /** Paint drawing the outline of the selector on the Canvas. */
        private val outlinePaint = Paint()

        /** The area where the selector should be drawn. */
        private var area: RectF = RectF()
        /** Difference between the center of the selector and its inner content. */
        private var selectorAreaOffset: Int = 0
        /** Area of the inner outline selector. */
        private var outlineInArea: RectF = RectF()
        /** Area of the outer outline selector. */
        private var outlineOutArea: RectF = RectF()

        /** Area within the selector that represents the zone to be capture to creates a click condition. */
        val selectedArea
            get() = area.toRect().apply {
                left += selectorAreaOffset
                top += selectorAreaOffset
                right -= selectorAreaOffset
                bottom -= selectorAreaOffset
            }

        init {
            val screenSize = windowManager.displaySize
            maxArea = RectF(0f, 0f, screenSize.x.toFloat(), screenSize.y.toFloat())
        }

        init {
            context.obtainStyledAttributes(R.style.OverlaySelectorView_Condition, R.styleable.ConditionSelectorView).use { ta ->
                val xOffset = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_defaultWidth, 100)
                    .toFloat() / 2
                val yOffset = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_defaultHeight, 100)
                    .toFloat() / 2
                area = RectF(maxArea.centerX() - xOffset, maxArea.centerY() - yOffset,
                    maxArea.centerX() + xOffset, maxArea.centerY() + yOffset)

                val thickness = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_thickness, 4).toFloat()
                val innerThickness = ta.getDimensionPixelSize(R.styleable.ConditionSelectorView_innerThickness, 2).toFloat()
                selectorAreaOffset = kotlin.math.ceil(thickness / 2).toInt() + 1
                outlinePaint.apply {
                    style = Paint.Style.STROKE
                    strokeWidth = (thickness - innerThickness) / 2
                    color = ta.getColor(R.styleable.ConditionSelectorView_colorOutlinePrimary, Color.RED)
                }
                paint.apply {
                    style = Paint.Style.STROKE
                    strokeWidth = innerThickness
                    color = ta.getColor(R.styleable.ConditionSelectorView_colorInner, Color.WHITE)
                }

                resizeDetector.resizeHandleSize = ta
                    .getDimensionPixelSize(R.styleable.ConditionSelectorView_resizeHandleSize, 10)
                    .toFloat()
            }

            invalidate()
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            return resizeDetector.onTouchEvent(event, area)
        }

        /**
         * Called when the [resizeDetector] detects a scale gesture.
         * Apply the scale factor to the selector and invalidate the view to redraw it.
         *
         * @param factor the scale factor detected.
         */
        private fun scale(factor: Float) {
            area.scale(factor)
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
                toX + area.width() > maxArea.right -> maxArea.right - area.width()
                else -> toX
            }
            val yPos = when {
                toY < maxArea.top -> maxArea.top
                toY + area.height() > maxArea.bottom -> maxArea.bottom - area.height()
                else -> toY
            }
            area.translate(xPos, yPos)
            invalidate()
        }

        /**
         * Called when the [resizeDetector] detects a resize gesture.
         * Apply the new size to the selector and invalidate the view to redraw it.
         *
         * @param newSize the new area of the selector after the resize.
         */
        private fun resize(newSize: RectF) {
            area = newSize
            invalidate()
        }

        override fun invalidate() {
            area.intersect(maxArea)
            outlineOutArea.apply {
                left = area.left - (paint.strokeWidth + outlinePaint.strokeWidth) / 2
                top = area.top - (paint.strokeWidth + outlinePaint.strokeWidth) / 2
                right = area.right + (paint.strokeWidth + outlinePaint.strokeWidth) / 2
                bottom = area.bottom + (paint.strokeWidth + outlinePaint.strokeWidth) / 2
            }
            outlineInArea.apply {
                left = area.left + (paint.strokeWidth + outlinePaint.strokeWidth) / 2
                top = area.top + (paint.strokeWidth + outlinePaint.strokeWidth) / 2
                right = area.right - (paint.strokeWidth + outlinePaint.strokeWidth) / 2
                bottom = area.bottom - (paint.strokeWidth + outlinePaint.strokeWidth) / 2
            }

            super.invalidate()
        }

        override fun onDraw(canvas: Canvas?) {
            canvas?.drawRect(outlineOutArea, outlinePaint)
            canvas?.drawRect(area, paint)
            canvas?.drawRect(outlineInArea, outlinePaint)
        }
    }
}
