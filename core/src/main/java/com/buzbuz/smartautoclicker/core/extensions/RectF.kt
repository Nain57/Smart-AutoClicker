/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.core.extensions

import android.graphics.RectF

/**
 * Scale the rectangle by the provided factor.
 * The pivot will be the center of the Rect.
 *
 * @param scaleFactor the scale factor.
 */
fun RectF.scale(scaleFactor: Float) {
    val xOffset = (width() * scaleFactor - width()) / 2
    val yOffset = (height() * scaleFactor - height()) / 2

    left -= xOffset
    top -= yOffset
    right += xOffset
    bottom += yOffset
}

/**
 * Translate to the specified position.
 *
 * @param toX the new x position.
 * @param toY the new y position.
 */
fun RectF.translate(toX: Float, toY: Float) {
    right = toX + width()
    bottom = toY + height()
    left = toX
    top = toY
}
