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
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

import androidx.annotation.ColorInt
import androidx.core.graphics.toRectF

import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer

import kotlin.math.max
import kotlin.math.min

internal class TextConditionBriefRenderer(
    briefView: View,
    viewStyle: TextConditionBriefRendererStyle,
) : ItemBriefRenderer<TextConditionBriefRendererStyle>(briefView, viewStyle) {

    private val selectorPaint = Paint().apply {
        isAntiAlias = true
        this.style = Paint.Style.STROKE
        strokeWidth = viewStyle.thicknessPx.toFloat()
        color = viewStyle.selectorColor
    }
    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = viewStyle.backgroundColor
    }

    private var detectionBorderRect: RectF? = null
    private val backgroundPath = Path().apply {
        fillType = Path.FillType.EVEN_ODD
    }

    private var briefDescription: TextConditionDescription? = null

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is TextConditionDescription) return
        briefDescription = description
    }

    override fun onInvalidate() {
        detectionBorderRect = null
        backgroundPath.reset()

        // Nothing to display ? Exit early
        val detectionArea = briefDescription?.conditionDetectionArea ?: return
        backgroundPath.addRectangleWithHole(
            RectF(0f, 0f, briefView.width.toFloat(), briefView.height.toFloat()),
            detectionArea.toRectF(),
        )
        detectionBorderRect = detectionArea.toDrawableRect(
            viewStyle.thicknessPx, briefView.width, briefView.height,
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (detectionBorderRect != null) {
            canvas.drawPath(backgroundPath, backgroundPaint)
            detectionBorderRect?.let { borderRect ->
                canvas.drawRoundRect(borderRect, viewStyle.cornerRadiusPx, viewStyle.cornerRadiusPx, selectorPaint)
            }
        }
    }

    override fun onStop() = Unit

    /** Get the position of this rectangle if it was centered in the provided container. */
    private fun Rect.centerIn(container: Rect): Rect {
        val offsetX = (container.width() - width()) / 2
        val offsetY = (container.height() - height()) / 2

        return Rect(
            container.left + offsetX,
            container.top + offsetY,
            container.right - offsetX,
            container.bottom - offsetY,
        )
    }

    /** Get the path to draw a rectangle containing a rectangle hole inside it.  */
    private fun Path.addRectangleWithHole(area: RectF, hole: RectF) {
        moveTo(area.left, area.top)
        lineTo(area.right,  area.top)
        lineTo(area.right, area.bottom)
        lineTo(area.left, area.bottom)
        close()

        moveTo(hole.left, hole.top)
        lineTo(hole.right, hole.top)
        lineTo(hole.right, hole.bottom)
        lineTo(hole.left, hole.bottom)
        close()
    }

    private fun Rect.toDrawableRect(thicknessPx: Int, maxWidth: Int, maxHeight: Int) = RectF(
        max(0, left - thicknessPx).toFloat(),
        max(0, top - thicknessPx).toFloat(),
        min(maxWidth, right + thicknessPx).toFloat(),
        min(maxHeight, bottom + thicknessPx).toFloat(),
    )
}

data class TextConditionDescription(
    val conditionText: String,
    val conditionDetectionArea: Rect,
) : ItemBriefDescription

internal data class TextConditionBriefRendererStyle(
    @field:ColorInt val backgroundColor: Int,
    @field:ColorInt val selectorColor: Int,
    val thicknessPx: Int,
    val cornerRadiusPx: Float,
)
