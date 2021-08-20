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
package com.buzbuz.smartautoclicker.baseui.overlayviews.condition.selector

import android.graphics.RectF

internal sealed class GestureType {
    /** */
    abstract fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float): RectF
    /** */
    fun isEnoughInnerSpace(viewArea: RectF, handleSize: Float) =
        viewArea.height() > handleSize && viewArea.width() > handleSize

}

internal object ResizeLeft: GestureType() {
    override fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float) =
        RectF(
            viewArea.left - handleSize,
            viewArea.top,
            if(isEnoughInnerSpace(viewArea, handleSize)) viewArea.left + innerHandleSize else viewArea.left,
            viewArea.bottom
        )
}

internal object ResizeTop: GestureType() {
    override fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float): RectF =
        RectF(
            viewArea.left,
            viewArea.top  - handleSize,
            viewArea.right,
            if (isEnoughInnerSpace(viewArea, handleSize)) viewArea.top + innerHandleSize else viewArea.top
        )
}

internal object ResizeRight: GestureType() {
    override fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float): RectF =
        RectF(
            if(isEnoughInnerSpace(viewArea, handleSize)) viewArea.right - innerHandleSize else viewArea.right,
            viewArea.top,
            viewArea.right + handleSize,
            viewArea.bottom
        )
}

internal object ResizeBottom: GestureType() {
    override fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float): RectF =
        RectF(
            viewArea.left,
            if (isEnoughInnerSpace(viewArea, handleSize)) viewArea.bottom - innerHandleSize else viewArea.bottom,
            viewArea.right,
            viewArea.bottom + handleSize
        )
}

internal object Move: GestureType() {
    override fun getGestureArea(viewArea: RectF, handleSize: Float, innerHandleSize: Float): RectF =
        viewArea
}