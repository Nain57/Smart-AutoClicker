/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.gesturerecord

import android.graphics.PointF
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ActionDescription
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ClickDescription
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.SwipeDescription


sealed class RecordedGesture {
    abstract val durationMs: Long

    data class Click(
        val position: PointF,
        override val durationMs: Long,
    ) : RecordedGesture()

    data class Swipe(
        val from: PointF,
        val to: PointF,
        override val durationMs: Long,
    ) : RecordedGesture()
}

fun RecordedGesture.toActionDescription(): ActionDescription =
    when (this) {
        is RecordedGesture.Click -> ClickDescription(pressDurationMs = durationMs, position = position)
        is RecordedGesture.Swipe -> SwipeDescription(swipeDurationMs = durationMs, from = from, to = to)
    }