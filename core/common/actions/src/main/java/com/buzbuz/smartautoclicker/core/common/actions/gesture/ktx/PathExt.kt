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
package com.buzbuz.smartautoclicker.core.common.actions.gesture.ktx

import android.graphics.Path
import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.extensions.safeLineTo
import com.buzbuz.smartautoclicker.core.base.extensions.safeMoveTo

internal fun Path.moveTo(position: Point): Unit =
    safeMoveTo(position.x, position.y)

internal fun Path.lineTo(position: Point): Unit =
    safeLineTo(position.x, position.y)

internal fun Path.line(from: Point, to: Point) {
    moveTo(from)
    lineTo(to)
}

/**
 * A zero length path. The framework handles it as a touch that doesn't move: the pointer stays down at [position]
 * for the whole stroke duration, without emitting a single move event.
 */
internal fun Path.hold(position: Point): Unit =
    moveTo(position)