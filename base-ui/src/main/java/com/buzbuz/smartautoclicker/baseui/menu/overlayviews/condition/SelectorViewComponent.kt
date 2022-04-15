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
package com.buzbuz.smartautoclicker.baseui.menu.overlayviews.condition

import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent

import androidx.annotation.CallSuper

import com.buzbuz.smartautoclicker.baseui.ScreenMetrics

/**
 * Base class for all view components displayed in the [ConditionSelectorView].
 *
 * @param screenMetrics provides information about current display.
 * @param viewInvalidator calls invalidate on the view hosting this component.
 */
internal abstract class SelectorViewComponent(
    private val screenMetrics: ScreenMetrics,
    private val viewInvalidator: () -> Unit,
) {

    /** The maximum size of the selector. */
    protected val maxArea: RectF = RectF().apply {
        val screenSize = screenMetrics.screenSize
        right = screenSize.x.toFloat()
        bottom = screenSize.y.toFloat()
    }

    /**
     * Called when the size of the [ConditionSelectorView] have changed.
     * Update the maximum area. Can be overridden to clear/adjust the displayed component position.
     *
     * @param w the width of the new view.
     * @param h the height of the new view.
     */
    @CallSuper
    open fun onViewSizeChanged(w: Int, h: Int) {
        val screenSize = screenMetrics.screenSize
        maxArea.apply {
            right = screenSize.x.toFloat()
            bottom = screenSize.y.toFloat()
        }
    }

    /**
     * Called when a touch event occurs in the [ConditionSelectorView].
     *
     * @param event the new touch event.
     * @return true if the event has been consumed, false if not.
     */
    abstract fun onTouchEvent(event: MotionEvent): Boolean

    /**
     * Called when the view needs to draw this component.
     *
     * @param canvas the canvas to draw in.
     */
    abstract fun onDraw(canvas: Canvas)

    /**
     * Called when this components needs to be reset (like after a cancel).
     * All temporary values should be dropped and the component should returns to its initial state.
     */
    abstract fun onReset()

    /** Invalidates the view containing the component. */
    protected fun invalidate() = viewInvalidator.invoke()
}