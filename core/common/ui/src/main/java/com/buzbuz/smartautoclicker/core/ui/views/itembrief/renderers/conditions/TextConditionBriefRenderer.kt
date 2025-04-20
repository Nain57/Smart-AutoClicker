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
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ScreenConditionBriefRenderingType

internal class TextConditionBriefRenderer(
    briefView: View,
    viewStyle: ScreenConditionBriefRendererStyle,
    displayConfigManager: DisplayConfigManager,
) : ScreenConditionBriefRenderer<TextConditionDescription>(briefView, viewStyle, displayConfigManager) {

    private var viewCenter = PointF(0f, 0f)
    private var iconDrawable: Drawable? = null


    override fun onInvalidate() {
        viewCenter = PointF(briefView.width / 2f, briefView.height / 2f)

        iconDrawable = briefDescription?.icon?.mutate()?.apply {
            setTint(viewStyle.selectorColor)
        }

        iconDrawable?.bounds = Rect(
            viewCenter.x.toInt() - viewStyle.iconSize.toInt(),
            viewCenter.y.toInt() - viewStyle.iconSize.toInt(),
            viewCenter.x.toInt() + viewStyle.iconSize.toInt(),
            viewCenter.y.toInt() + viewStyle.iconSize.toInt(),
        )

        super.onInvalidate()
    }
    override fun onDraw(canvas: Canvas) {
        iconDrawable?.draw(canvas)
        super.onDraw(canvas)
    }
}

data class TextConditionDescription(
    override val detectionAreaType: ScreenConditionBriefRenderingType,
    override val detectionArea: Rect?,
    val icon: Drawable? = null,
) : ScreenConditionDescription()