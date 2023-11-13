/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief.DumbActionDescription

class DumbActionBriefView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Paint drawing the outer circle of the position 1. */
    private val style: DumbActionBriefViewStyle

    init {
        if (attrs == null) throw IllegalArgumentException("AttributeSet is null")
        style = context.getDumbActionBriefStyle(attrs, defStyleAttr)
    }

    private var renderer: DumbActionBriefRenderer? = null

    fun setDescription(dumbActionDescription: DumbActionDescription?) {
        renderer?.onStop()

        if (dumbActionDescription == null) {
            invalidate()
            return
        }

        renderer = when (dumbActionDescription) {
            is DumbActionDescription.Click -> DumbClickBriefRenderer(this, style, ::invalidate)
            is DumbActionDescription.Swipe -> DumbSwipeBriefRenderer(this, style, ::invalidate)
            is DumbActionDescription.Pause -> DumbPauseBriefRenderer(this, style, ::invalidate)
        }

        renderer?.onNewDescription(dumbActionDescription)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) renderer?.onSizeChanged(w, h)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderer?.onDraw(canvas)
    }
}

internal abstract class DumbActionBriefRenderer(
    protected val briefView: View,
    protected val style: DumbActionBriefViewStyle,
    private val viewInvalidator: () -> Unit,
) {
    abstract fun onNewDescription(description: DumbActionDescription)
    open fun onSizeChanged(w: Int, h: Int) = Unit
    abstract fun onDraw(canvas: Canvas)
    abstract fun onStop()
    protected fun invalidate() { viewInvalidator() }
}
