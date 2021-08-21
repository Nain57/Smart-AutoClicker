/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.baseui.overlayviews.condition

import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent
import androidx.annotation.CallSuper

import com.buzbuz.smartautoclicker.extensions.ScreenMetrics

/** */
internal abstract class SelectorViewComponent(
    private val screenMetrics: ScreenMetrics,
    private val viewInvalidator: () -> Unit,
) {

    /** The maximum size of the selector. */
    protected val maxArea: RectF = RectF().apply {
        val screenSize = screenMetrics.getScreenSize()
        right = screenSize.x.toFloat()
        bottom = screenSize.y.toFloat()
    }

    /** */
    @CallSuper
    open fun onViewSizeChanged(w: Int, h: Int) {
        val screenSize = screenMetrics.getScreenSize()
        maxArea.apply {
            right = screenSize.x.toFloat()
            bottom = screenSize.y.toFloat()
        }
    }

    /** */
    abstract fun onTouchEvent(event: MotionEvent): Boolean

    /** */
    abstract fun onDraw(canvas: Canvas)

    /** */
    abstract fun onReset()

    /**
     *
     */
    protected fun invalidate() = viewInvalidator.invoke()
}