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
package com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.base

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

abstract class ComponentsView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr), ViewInvalidator {

    internal abstract val viewComponents: List<ViewComponent>

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewComponents.forEach { viewComponent -> viewComponent.onViewSizeChanged(w, h) }
        invalidate()
    }

    override fun invalidate() {
        viewComponents.forEach { viewComponent -> viewComponent.onInvalidate() }
        super.invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        viewComponents.forEach { viewComponent -> viewComponent.onDraw(canvas) }
    }
}

interface ViewInvalidator {
    fun invalidate()
}