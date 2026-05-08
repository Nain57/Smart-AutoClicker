/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF


data class LineF(val start: PointF = PointF(), val end: PointF = PointF()) {

    fun setStart(x: Float, y: Float) {
        start.set(x, y)
    }

    fun setEnd(x: Float, y: Float) {
        end.set(x, y)
    }

    fun clear() {
        setStart(0f, 0f)
        setEnd(0f, 0f)
    }
}

fun Canvas.drawLine(line: LineF, paint: Paint) =
    drawLine(line.start.x, line.start.y, line.end.x, line.end.y, paint)

