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

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View

import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefDescription
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefRenderer

internal class DefaultBriefRenderer(
    briefView: View,
    viewStyle: DefaultBriefRendererStyle,
) : ItemBriefRenderer<DefaultBriefRendererStyle>(briefView, viewStyle) {

    private val gradientBackgroundPaint: Paint = Paint()

    private var viewCenter = PointF(0f, 0f)
    private var iconDrawable: Drawable? = null

    override fun onNewDescription(description: ItemBriefDescription, animate: Boolean) {
        if (description !is DefaultDescription) return

        iconDrawable = description.icon?.mutate()?.apply {
            setTint(viewStyle.iconColor)
        }
    }

    override fun onInvalidate() {
        viewCenter = PointF(briefView.width / 2f, briefView.height / 2f)

        iconDrawable?.bounds = Rect(
            viewCenter.x.toInt() - viewStyle.iconSize.toInt(),
            viewCenter.y.toInt() - viewStyle.iconSize.toInt(),
            viewCenter.x.toInt() + viewStyle.iconSize.toInt(),
            viewCenter.y.toInt() + viewStyle.iconSize.toInt(),
        )

        gradientBackgroundPaint.shader = createRadialGradientShader(
            position = viewCenter,
            radius = viewStyle.iconSize * 1.75f,
            color = viewStyle.backgroundColor,
        )
    }

    override fun onDraw(canvas: Canvas) {
        canvas.apply {
            drawCircle(viewCenter.x, viewCenter.y, viewStyle.iconSize * 2f, gradientBackgroundPaint)
            iconDrawable?.draw(canvas)
        }
    }

    override fun onStop() = Unit
}

internal data class DefaultBriefRendererStyle(
    @ColorInt val backgroundColor: Int,
    @ColorInt val iconColor: Int,
    val iconSize: Float,
    val outerPaint: Paint,
)

data class DefaultDescription(
    val icon: Drawable? = null,
) : ItemBriefDescription