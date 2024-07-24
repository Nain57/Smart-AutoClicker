/*
 * Copyright (C) 2024 Kevin Buzeau
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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer

import kotlin.math.max
import kotlin.math.min

internal class ImageConditionBriefRenderer(
    briefView: View,
    viewStyle: ImageConditionBriefRendererStyle,
) : ItemBriefRenderer<ImageConditionBriefRendererStyle>(briefView, viewStyle) {

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

    private var imagePosition: Rect? = null
    private var detectionBorderRect: RectF? = null
    private val backgroundPath = Path().apply {
        fillType = Path.FillType.EVEN_ODD
    }

    private var briefDescription: ImageConditionDescription? = null

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is ImageConditionDescription) return

        briefDescription = description
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int) {
        super.onSizeChanged(w, h)
        invalidate()
    }

    override fun invalidate() {
        imagePosition = null
        detectionBorderRect = null
        backgroundPath.reset()

        val position = briefDescription?.conditionPosition
        val detectionArea = briefDescription?.conditionDetectionArea
        if (detectionArea == null || position == null) {
            super.invalidate()
            return
        }

        imagePosition =
            if (detectionArea == position) position
            else {
                val offsetX = (detectionArea.width() - position.width()) / 2
                val offsetY = (detectionArea.height() - position.height()) / 2
                Rect(
                    detectionArea.left + offsetX,
                    detectionArea.top + offsetY,
                    detectionArea.right - offsetX,
                    detectionArea.bottom - offsetY,
                )
            }

        backgroundPath.apply {
            moveTo(0f, 0f)
            lineTo(briefView.width.toFloat(), 0f)
            lineTo(briefView.width.toFloat(), briefView.height.toFloat())
            lineTo(0f, briefView.height.toFloat())
            close()

            moveTo(detectionArea.left.toFloat(), detectionArea.top.toFloat())
            lineTo(detectionArea.right.toFloat(), detectionArea.top.toFloat())
            lineTo(detectionArea.right.toFloat(), detectionArea.bottom.toFloat())
            lineTo(detectionArea.left.toFloat(), detectionArea.bottom.toFloat())
            close()
        }

        detectionBorderRect = RectF(
            max(0, detectionArea.left - viewStyle.thicknessPx).toFloat(),
            max(0, detectionArea.top - viewStyle.thicknessPx).toFloat(),
            min(briefView.width, detectionArea.right + viewStyle.thicknessPx).toFloat(),
            min(briefView.height, detectionArea.bottom + viewStyle.thicknessPx).toFloat(),
        )

        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        briefDescription?.let { description ->
            canvas.drawPath(backgroundPath, backgroundPaint)

            description.conditionBitmap?.let { bitmap ->
                imagePosition?.let { position ->
                    canvas.drawBitmap(bitmap, null, position, null)
                }
            }

            detectionBorderRect?.let { borderRect ->
                canvas.drawRoundRect(borderRect, viewStyle.cornerRadiusPx, viewStyle.cornerRadiusPx, selectorPaint)
            }
        }
    }

    override fun onStop() = Unit
}

data class ImageConditionDescription(
    val conditionBitmap: Bitmap?,
    val conditionPosition: Rect,
    val conditionDetectionArea: Rect?,
) : ItemBriefDescription

internal data class ImageConditionBriefRendererStyle(
    @ColorInt val backgroundColor: Int,
    @ColorInt val selectorColor: Int,
    val thicknessPx: Int,
    val cornerRadiusPx: Float,
)
