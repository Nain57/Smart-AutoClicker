/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.conditions

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
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ScreenConditionBriefRenderingType
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DisplayBorderComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DisplayBorderComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewInvalidator

import kotlin.math.max
import kotlin.math.min

internal abstract class ScreenConditionBriefRenderer<T : ScreenConditionDescription>(
    briefView: View,
    viewStyle: ScreenConditionBriefRendererStyle,
    displayConfigManager: DisplayConfigManager,
) : ItemBriefRenderer<ScreenConditionBriefRendererStyle>(briefView, viewStyle) {

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


    private var detectionBorderRect: RectF? = null
    private val backgroundPath = Path().apply {
        fillType = Path.FillType.EVEN_ODD
    }

    protected var briefDescription: T? = null

    @Suppress("UNCHECKED_CAST")
    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        briefDescription = description as T
    }

    override fun onInvalidate() {
        detectionBorderRect = null
        backgroundPath.reset()
        displayBorderComponent.onReset()

        // Nothing to display ? Exit early
        val detectionArea = briefDescription?.detectionArea
        val detectionType = briefDescription?.detectionAreaType ?: return

        when (detectionType) {
            ScreenConditionBriefRenderingType.AREA -> {
                detectionArea ?: return

                backgroundPath.addRectangleWithHole(
                    RectF(0f, 0f, briefView.width.toFloat(), briefView.height.toFloat()),
                    detectionArea.toRectF(),
                )
                detectionBorderRect = detectionArea.toDrawableRect(
                    viewStyle.thicknessPx, briefView.width, briefView.height,
                )
            }

            ScreenConditionBriefRenderingType.EXACT -> {
                detectionArea ?: return

                backgroundPath.addRectangleWithHole(
                    RectF(0f, 0f, briefView.width.toFloat(), briefView.height.toFloat()),
                    detectionArea.toRectF(),
                )
                detectionBorderRect = detectionArea.toDrawableRect(
                    viewStyle.thicknessPx, briefView.width, briefView.height,
                )
            }

            ScreenConditionBriefRenderingType.WHOLE_SCREEN -> {
                displayBorderComponent.onInvalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
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


sealed class ScreenConditionDescription : ItemBriefDescription {
    abstract val detectionAreaType: ScreenConditionBriefRenderingType
    abstract val detectionArea: Rect?
}

internal data class ScreenConditionBriefRendererStyle(
    @ColorInt val backgroundColor: Int,
    @ColorInt val selectorColor: Int,
    val iconSize: Float,
    val thicknessPx: Int,
    val cornerRadiusPx: Float,
)
