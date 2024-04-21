/*
 * Copyright (C) 2022 Kevin Buzeau
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

import android.graphics.RectF

/** Types of gestures. */
internal sealed class GestureType {
    /**
     * Get the touch area for this gesture.
     *
     * @param viewArea the area of the selector
     * @param handleSize the size of the view handle
     * @param innerHandleSize the inner size of the handle.
     */
    open fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float): RectF =
        viewArea

    /**
     * Tells if the selector is big enough to consider using part of it as gesture area.
     *
     * @param viewArea the area of the view
     * @param handleSize the size of the handle.
     * @return true if you can use part of the selector as gesture area, false if not.
     */
    fun isEnoughInnerSpace(viewArea: RectF, handleSize: Float) =
        viewArea.height() > handleSize && viewArea.width() > handleSize

}

internal data object ResizeLeft: GestureType() {
    override fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float) =
        RectF(
            viewArea.left - handleSize,
            viewArea.top,
            if(isEnoughInnerSpace(viewArea, handleSize)) viewArea.left + innerHandleSize else viewArea.left,
            viewArea.bottom
        )
}

internal data object ResizeTop: GestureType() {
    override fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float): RectF =
        RectF(
            viewArea.left,
            viewArea.top  - handleSize,
            viewArea.right,
            if (isEnoughInnerSpace(viewArea, handleSize)) viewArea.top + innerHandleSize else viewArea.top
        )
}

internal data object ResizeRight: GestureType() {
    override fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float): RectF =
        RectF(
            if(isEnoughInnerSpace(viewArea, handleSize)) viewArea.right - innerHandleSize else viewArea.right,
            viewArea.top,
            viewArea.right + handleSize,
            viewArea.bottom
        )
}

internal data object ResizeBottom: GestureType() {
    override fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float): RectF =
        RectF(
            viewArea.left,
            if (isEnoughInnerSpace(viewArea, handleSize)) viewArea.bottom - innerHandleSize else viewArea.bottom,
            viewArea.right,
            viewArea.bottom + handleSize
        )
}

internal data object MoveSelector: GestureType()

internal data object ZoomCapture: GestureType()