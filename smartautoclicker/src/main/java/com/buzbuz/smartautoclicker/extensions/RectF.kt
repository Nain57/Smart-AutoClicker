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
package com.buzbuz.smartautoclicker.extensions

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


/**
 * Tells if the offset rectangle on the left is containing the provided position.
 *
 * The offset rectangle of width equals to the offset parameter next to the left edge of this Rect. Its height will vary
 * between this Rect height and this Rect height + 2 * offset if this Rect width > height.
 *
 * @param offset the size of te offset Rect.
 * @param x the x position to be verified.
 * @param y the y position to be verified.
 *
 * @return true if the position is contained in the offset Rect, false if not.
 */
fun RectF.leftOffsetContains(offset: Float, x: Float, y: Float) : Boolean =
    left - offset < x && x < left && if (width() > height()) {
        top - offset < y && y < bottom + offset
    } else {
        top < y && y < bottom
    }

/**
 * Tells if the offset rectangle on the top is containing the provided position.
 *
 * The offset rectangle of width equals to the offset parameter next to the top edge of this Rect. Its width will vary
 * between this Rect width + 2 * offset and Rect width if this Rect width > height.
 *
 * @param offset the size of te offset Rect.
 * @param x the x position to be verified.
 * @param y the y position to be verified.
 *
 * @return true if the position is contained in the offset Rect, false if not.
 */
fun RectF.topOffsetContains(offset: Float, x: Float, y: Float) : Boolean =
    top - offset < y && y < top && if (width() > height()) {
        left < x && x < right
    } else {
        left - offset < x && x < right + offset
    }

/**
 * Tells if the offset rectangle on the right is containing the provided position.
 *
 * The offset rectangle of width equals to the offset parameter next to the right edge of this Rect. Its height will vary
 * between this Rect height and this Rect height + 2 * offset if this Rect width > height.
 *
 * @param offset the size of te offset Rect.
 * @param x the x position to be verified.
 * @param y the y position to be verified.
 *
 * @return true if the position is contained in the offset Rect, false if not.
 */
fun RectF.rightOffsetContains(offset: Float, x: Float, y: Float) : Boolean =
    right < x && x < right + offset && if (width() > height()) {
        top - offset < y && y < bottom + offset
    } else {
        top < y && y < bottom
    }

/**
 * Tells if the offset rectangle on the bottom is containing the provided position.
 *
 * The offset rectangle of width equals to the offset parameter next to the bottom edge of this Rect. Its width will
 * vary between this Rect width + 2 * offset and Rect width if this Rect width > height.
 *
 * @param offset the size of te offset Rect.
 * @param x the x position to be verified.
 * @param y the y position to be verified.
 *
 * @return true if the position is contained in the offset Rect, false if not.
 */
fun RectF.bottomOffsetContains(offset: Float, x: Float, y: Float) : Boolean =
    bottom < y && y < bottom + offset && if (width() > height()) {
        left < x && x < right
    } else {
        left - offset < x && x < right + offset
    }

