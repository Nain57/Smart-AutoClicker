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
package com.buzbuz.smartautoclicker.core.ui.views.actionbrief

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ActionBriefView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Paint drawing the outer circle of the position 1. */
    private val style: ActionBriefViewStyle

    init {
        if (attrs == null) throw IllegalArgumentException("AttributeSet is null")
        style = context.getActionBriefStyle(attrs, defStyleAttr)
    }

    private var renderer: ActionBriefRenderer? = null

    /** Listener upon touch events */
    var onTouchListener: ((position: PointF) -> Unit)? = null

    fun setDescription(description: ActionDescription?) {
        renderer?.onStop()

        if (description == null) {
            invalidate()
            return
        }

        renderer = when (description) {
            is ClickDescription -> ClickBriefRenderer(this, style, ::invalidate)
            is SwipeDescription -> SwipeBriefRenderer(this, style, ::invalidate)
            is PauseDescription -> PauseBriefRenderer(this, style, ::invalidate)
            else -> return
        }

        renderer?.onNewDescription(description)
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility") // You can't click on this view
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        onTouchListener?.invoke(event.getValidPosition()) ?: return false

        return true
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

    /** Get the position of the motion event and ensure it is within screen bounds. */
    private fun MotionEvent.getValidPosition(): PointF =
        PointF(
            x.coerceIn(0f, width.toFloat()),
            y.coerceIn(0f, height.toFloat()),
        )
}

interface ActionDescription