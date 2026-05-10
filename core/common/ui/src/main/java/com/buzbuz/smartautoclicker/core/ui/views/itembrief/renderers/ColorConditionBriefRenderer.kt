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
package com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.View

import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.ui.utils.LineF
import com.buzbuz.smartautoclicker.core.ui.utils.drawLine
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer

internal class ColorConditionBriefRenderer(
    briefView: View,
    viewStyle: ColorConditionBriefRendererStyle,
) : ItemBriefRenderer<ColorConditionBriefRendererStyle>(briefView, viewStyle) {

    private val linePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = viewStyle.selectorColor
        strokeWidth = 5.toFloat()
    }

    private val horizontalLine: LineF = LineF()
    private val verticalLine: LineF = LineF()

    private var briefDescription: ColorConditionDescription? = null

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is ColorConditionDescription) return

        briefDescription = description
    }

    override fun onInvalidate() {
        val viewSize = getViewSize()
        val conditionPosition = briefDescription?.conditionPosition ?: return

        horizontalLine.setStart(0f, conditionPosition.y)
        horizontalLine.setEnd(viewSize.x.toFloat(), conditionPosition.y)

        verticalLine.setStart(conditionPosition.x, 0f)
        verticalLine.setEnd(conditionPosition.x, viewSize.y.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawLine(horizontalLine, linePaint)
        canvas.drawLine(verticalLine, linePaint)
    }

    override fun onStop() {
        horizontalLine.clear()
        verticalLine.clear()
    }
}

data class ColorConditionDescription(
    @field:ColorInt val conditionColor: Int,
    val conditionPosition: PointF,
) : ItemBriefDescription

internal data class ColorConditionBriefRendererStyle(
    @field:ColorInt val backgroundColor: Int,
    @field:ColorInt val selectorColor: Int,
    val thicknessPx: Int,
)
