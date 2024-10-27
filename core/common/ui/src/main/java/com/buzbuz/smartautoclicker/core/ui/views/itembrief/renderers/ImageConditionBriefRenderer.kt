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
import androidx.core.graphics.toRectF

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DisplayBorderComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DisplayBorderComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewInvalidator

import kotlin.math.max
import kotlin.math.min

internal class ImageConditionBriefRenderer(
    briefView: View,
    viewStyle: ImageConditionBriefRendererStyle,
    displayConfigManager: DisplayConfigManager,
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

    private val displayBorderComponent = DisplayBorderComponent(
        viewStyle = DisplayBorderComponentStyle(
            displayConfigManager = displayConfigManager,
            color = viewStyle.selectorColor,
            thicknessPx = viewStyle.thicknessPx,
        ),
        viewInvalidator = object : ViewInvalidator {
            override fun invalidate() { invalidateView() }
        },
    )

    private var imagePosition: Rect? = null
    private var detectionBorderRect: RectF? = null
    private val backgroundPath = Path().apply {
        fillType = Path.FillType.EVEN_ODD
    }

    private var briefDescription: ImageConditionDescription? = null

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is ImageConditionDescription) return

        briefDescription = description
    }

    override fun onInvalidate() {
        imagePosition = null
        detectionBorderRect = null
        backgroundPath.reset()
        displayBorderComponent.onReset()

        // Nothing to display ? Exit early
        val position = briefDescription?.conditionPosition
        val detectionArea = briefDescription?.conditionDetectionArea
        val detectionType = briefDescription?.conditionDetectionType
        if (detectionArea == null || position == null || detectionType == null) {
            return
        }

        when (detectionType) {
            ImageConditionBriefRenderingType.AREA -> {
                imagePosition = position.centerIn(detectionArea)
                backgroundPath.addRectangleWithHole(
                    RectF(0f, 0f, briefView.width.toFloat(), briefView.height.toFloat()),
                    detectionArea.toRectF(),
                )
                detectionBorderRect = detectionArea.toDrawableRect(
                    viewStyle.thicknessPx, briefView.width, briefView.height,
                )
            }

            ImageConditionBriefRenderingType.EXACT -> {
                imagePosition = position
                backgroundPath.addRectangleWithHole(
                    RectF(0f, 0f, briefView.width.toFloat(), briefView.height.toFloat()),
                    detectionArea.toRectF(),
                )
                detectionBorderRect = detectionArea.toDrawableRect(
                    viewStyle.thicknessPx, briefView.width, briefView.height,
                )
            }

            ImageConditionBriefRenderingType.WHOLE_SCREEN -> {
                imagePosition = position.centerIn(detectionArea)
                displayBorderComponent.onInvalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        imagePosition?.let { position ->
            briefDescription?.conditionBitmap?.let { bitmap ->
                canvas.drawBitmap(bitmap, null, position, null)
            }
        }

        if (detectionBorderRect != null) {
            canvas.drawPath(backgroundPath, backgroundPaint)
            detectionBorderRect?.let { borderRect ->
                canvas.drawRoundRect(borderRect, viewStyle.cornerRadiusPx, viewStyle.cornerRadiusPx, selectorPaint)
            }
        } else {
            displayBorderComponent.onDraw(canvas)
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

data class ImageConditionDescription(
    val conditionBitmap: Bitmap?,
    val conditionDetectionType: ImageConditionBriefRenderingType,
    val conditionPosition: Rect,
    val conditionDetectionArea: Rect?,
) : ItemBriefDescription

enum class ImageConditionBriefRenderingType {
    AREA,
    EXACT,
    WHOLE_SCREEN
}

internal data class ImageConditionBriefRendererStyle(
    @ColorInt val backgroundColor: Int,
    @ColorInt val selectorColor: Int,
    val thicknessPx: Int,
    val cornerRadiusPx: Float,
)
