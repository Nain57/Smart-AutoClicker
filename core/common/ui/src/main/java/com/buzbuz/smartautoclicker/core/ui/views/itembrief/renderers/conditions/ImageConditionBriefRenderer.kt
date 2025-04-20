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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import com.buzbuz.smartautoclicker.core.base.extensions.centerIn

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ScreenConditionBriefRenderingType

internal class ImageConditionBriefRenderer(
    briefView: View,
    viewStyle: ScreenConditionBriefRendererStyle,
    displayConfigManager: DisplayConfigManager,
) : ScreenConditionBriefRenderer<ImageConditionDescription>(briefView, viewStyle, displayConfigManager) {

    private var imagePosition: Rect? = null

    override fun onInvalidate() {
        imagePosition = null

        val position = briefDescription?.conditionPosition ?: return
        val areaType = briefDescription?.detectionAreaType ?: return
        val area = briefDescription?.detectionArea

        imagePosition = when {
            areaType == ScreenConditionBriefRenderingType.EXACT -> position
            area != null -> position.centerIn(area)
            else -> null
        }

        super.onInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        imagePosition?.let { position ->
            briefDescription?.conditionBitmap?.let { bitmap ->
                canvas.drawBitmap(bitmap, null, position, null)
            }
        }

        super.onDraw(canvas)
    }
}

data class ImageConditionDescription(
    override val detectionAreaType: ScreenConditionBriefRenderingType,
    override val detectionArea: Rect?,
    val conditionBitmap: Bitmap?,
    val conditionPosition: Rect,
) : ScreenConditionDescription()