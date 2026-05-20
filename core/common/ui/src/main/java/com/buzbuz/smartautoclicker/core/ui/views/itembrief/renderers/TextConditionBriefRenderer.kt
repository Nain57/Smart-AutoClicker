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

    private companion object {
        /** Maximal size of the condition text. */
        private const val MAX_TEXT_SIZE = 48f
        /** Margin between the text and the border rect. */
        private const val TEXT_MARGIN_PX = 8f
    }

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
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = viewStyle.selectorColor
        textAlign = Paint.Align.CENTER
    }

    private var detectionBorderRect: RectF? = null
    private val backgroundPath = Path().apply {
        fillType = Path.FillType.EVEN_ODD
    }

    private var briefDescription: TextConditionDescription? = null
    private var textToShow: String? = null
    private var textX = 0f
    private var textY = 0f

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is TextConditionDescription) return
        briefDescription = description
    }

    override fun onInvalidate() {
        detectionBorderRect = null
        backgroundPath.reset()
        textToShow = null

        // Nothing to display ? Exit early
        val description = briefDescription ?: return
        val detectionArea = description.conditionDetectionArea
        backgroundPath.addRectangleWithHole(
            RectF(0f, 0f, briefView.width.toFloat(), briefView.height.toFloat()),
            detectionArea.toRectF(),
        )
        val borderRect = detectionArea.toDrawableRect(
            viewStyle.thicknessPx, briefView.width, briefView.height,
        )
        detectionBorderRect = borderRect

        val text = description.conditionText
        if (text.isEmpty()) return

        val margin = TEXT_MARGIN_PX + viewStyle.thicknessPx / 2f
        val maxWidth = borderRect.width() - 2 * margin
        val maxHeight = borderRect.height() - 2 * margin
        if (maxWidth <= 0 || maxHeight <= 0) return

        textPaint.textSize = MAX_TEXT_SIZE
        val bounds = Rect()
        textPaint.getTextBounds(text, 0, text.length, bounds)

        val widthScale = maxWidth / bounds.width()
        val heightScale = maxHeight / bounds.height()
        val scale = min(1f, min(widthScale, heightScale))

        textPaint.textSize = MAX_TEXT_SIZE * scale
        textX = borderRect.centerX()
        textY = borderRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        textToShow = text
    }

    override fun onDraw(canvas: Canvas) {
        val borderRect = detectionBorderRect ?: return

        canvas.drawPath(backgroundPath, backgroundPaint)
        canvas.drawRoundRect(borderRect, viewStyle.cornerRadiusPx, viewStyle.cornerRadiusPx, selectorPaint)

        textToShow?.let { text ->
            canvas.drawText(text, textX, textY, textPaint)
        }
    }

    override fun onStop() = Unit

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
