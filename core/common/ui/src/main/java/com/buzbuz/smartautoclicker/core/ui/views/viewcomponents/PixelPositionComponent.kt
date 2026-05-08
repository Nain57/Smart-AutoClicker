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
package com.buzbuz.smartautoclicker.core.ui.views.viewcomponents

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.utils.LineF
import com.buzbuz.smartautoclicker.core.ui.utils.drawLine
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewComponent
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewInvalidator
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base.ViewStyle

internal class PixelPositionComponent(
    private val viewStyle: PixelPositionComponentStyle,
    viewInvalidator: ViewInvalidator,
): ViewComponent(viewStyle, viewInvalidator) {

    private val linePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = viewStyle.color
        strokeWidth = viewStyle.thicknessPx
    }

    private val horizontalLine: LineF = LineF()
    private val verticalLine: LineF = LineF()

    val pixelPosition: PointF = PointF()


    override fun onTouchEvent(event: MotionEvent): Boolean {
        pixelPosition.set(event.x, event.y)
        return true
    }

    override fun onInvalidate() {
        horizontalLine.setStart(0f, pixelPosition.y)
        horizontalLine.setEnd(maxArea.width(), pixelPosition.y)

        verticalLine.setStart(pixelPosition.x, 0f)
        verticalLine.setEnd(pixelPosition.x, maxArea.height())
    }

    override fun onReset() {
        pixelPosition.set(0f, 0f)
        horizontalLine.clear()
        verticalLine.clear()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawLine(horizontalLine, linePaint)
        canvas.drawLine(verticalLine, linePaint)
    }
}

internal class PixelPositionComponentStyle(
    displayConfigManager: DisplayConfigManager,
    @field:ColorInt val color: Int,
    val thicknessPx: Float,
) : ViewStyle(displayConfigManager)