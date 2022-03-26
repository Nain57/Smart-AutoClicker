/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.overlays.mainmenu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View

/**
 * Displays a rectangle at the selected position to represents the detection.
 * @param context the Android context.
 */
class DebugView(context: Context) : View(context) {

    /** The paint for the condition border. */
    private val conditionBordersPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    /** The margin between the actual condition position and the displayed borders. */
    private val conditionBordersMargin = 10
    /** The position of the borders. */
    private val conditionBorders = Rect()

    /**
     * Set the position of a new positive detection result.
     * @param position the position of the detected condition.
     */
    fun setPositiveResult(position: Rect) {
        updatePositiveResult(position)
        postInvalidate()
    }

    /** Clear all values and display nothing. */
    fun clear() {
        updatePositiveResult(Rect(), true)
        postInvalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        updatePositiveResult(forceUpdate = true)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    /**
     * Update the position of the displayed borders.
     * @param newConditionPosition the new coordinates of the borders.
     * @param forceUpdate true to force the refresh of the positions, false to skip it if not necessary.
     */
    private fun updatePositiveResult(newConditionPosition: Rect = conditionBorders, forceUpdate: Boolean = false) {
        // Same values ? Skip update, unless forced.
        if (!forceUpdate && newConditionPosition == conditionBorders) return

        // No condition matched ? Nothing to display
        if (newConditionPosition == Rect()) {
            conditionBorders.set(0, 0, 0, 0)
            return
        }

        // Condition borders update
        if (forceUpdate || newConditionPosition != conditionBorders) {
            conditionBorders.left = newConditionPosition.left - conditionBordersMargin
            conditionBorders.top = newConditionPosition.top - conditionBordersMargin
            conditionBorders.right = newConditionPosition.right + conditionBordersMargin
            conditionBorders.bottom = newConditionPosition.bottom + conditionBordersMargin
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawRect(conditionBorders, conditionBordersPaint)
    }
}
