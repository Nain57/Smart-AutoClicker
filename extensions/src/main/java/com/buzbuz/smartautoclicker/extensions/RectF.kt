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
package com.buzbuz.smartautoclicker.extensions

import android.graphics.PointF
import android.graphics.RectF

fun RectF.scale(scaleFactor: Float, pivot: PointF) {
    val widthOffset = (width() * scaleFactor - width())
    val heightOffset = (height() * scaleFactor - height())

    val pivotRatioX = (pivot.x - left) / width()
    val pivotRatioY = (pivot.y - top) / height()

    left -= widthOffset * pivotRatioX
    top -= heightOffset * pivotRatioY
    right += widthOffset * (1 - pivotRatioX)
    bottom += heightOffset * (1 - pivotRatioY)
}

fun RectF.translate(translateX: Float, translateY: Float) {
    left += translateX
    top += translateY
    right += translateX
    bottom += translateY
}
