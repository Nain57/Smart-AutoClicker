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
package com.buzbuz.smartautoclicker.core.base.extensions

import android.graphics.Point
import android.graphics.Rect

/**
 * Get the width and height of this Rect.
 *
 * @return the size.
 */
fun Rect.size(): Point = Point(width(), height())

/** Ensure the rectangle is at least the provided minimal width and height. */
fun Rect.ensureMinSize(minWidth: Int = 1, minHeight: Int = 1): Rect =
    Rect(
        left,
        top,
        if (left != 0 && right != 0 && left == right) left + minWidth else right,
        if (top != 0 && bottom != 0 && top == bottom) top + minWidth else bottom,
    )