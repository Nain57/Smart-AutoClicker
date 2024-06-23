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
import android.graphics.Rect
import android.view.View
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer


internal class ImageConditionBriefRenderer(
    briefView: View,
    viewStyle: ImageConditionBriefRendererStyle,
) : ItemBriefRenderer<ImageConditionBriefRendererStyle>(briefView, viewStyle) {

    private val detectionAreaPaint = Paint().apply {
        isAntiAlias = true
        this.style = Paint.Style.FILL
        color = viewStyle.detectionAreaColor
    }
    private val detectionAreaRectList: MutableList<Rect> = mutableListOf()

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
        updateDetectionArea()
        super.invalidate()
    }

    private fun updateDetectionArea() = detectionAreaRectList.apply {
        clear()
        val area = briefDescription?.conditionDetectionArea ?: return@apply

        // Top Left
        add(Rect(area.left - viewStyle.thicknessPx, area.top - viewStyle.thicknessPx,
            area.left + viewStyle.lengthPx, area.top))
        add(Rect(area.left - viewStyle.thicknessPx, area.top - viewStyle.thicknessPx,
            area.left, area.top + viewStyle.lengthPx))
        // Top Right
        add(Rect(area.right - viewStyle.lengthPx, area.top - viewStyle.thicknessPx,
            area.right + viewStyle.thicknessPx, area.top))
        add(Rect(area.right, area.top - viewStyle.thicknessPx,
            area.right + viewStyle.thicknessPx, area.top + viewStyle.lengthPx))
        // Bottom Left
        add(Rect(area.left - viewStyle.thicknessPx, area.bottom,
            area.left + viewStyle.lengthPx, area.bottom + viewStyle.thicknessPx))
        add(Rect(area.left - viewStyle.thicknessPx, area.bottom - viewStyle.lengthPx,
            area.left, area.bottom + viewStyle.thicknessPx))
        // Bottom Right
        add(Rect(area.right - viewStyle.lengthPx, area.bottom,
            area.right + viewStyle.thicknessPx, area.bottom + viewStyle.thicknessPx))
        add(Rect(area.right, area.bottom - viewStyle.lengthPx,
            area.right + viewStyle.thicknessPx, area.bottom + viewStyle.thicknessPx))
    }

    override fun onDraw(canvas: Canvas) {
        briefDescription?.let { description ->
            description.conditionBitmap?.let { bitmap ->
                canvas.drawBitmap(bitmap, null, description.conditionPosition, null)
            }

            detectionAreaRectList.forEach { rect ->
                canvas.drawRect(rect, detectionAreaPaint)
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
    @ColorInt val detectionAreaColor: Int,
    val thicknessPx: Int,
    val lengthPx: Int,
)
