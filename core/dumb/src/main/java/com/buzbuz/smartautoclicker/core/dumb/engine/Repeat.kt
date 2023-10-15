/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.dumb.engine

import com.buzbuz.smartautoclicker.core.dumb.domain.model.Repeatable
import com.buzbuz.smartautoclicker.core.dumb.domain.model.RepeatableWithDelay
import kotlinx.coroutines.delay

internal suspend fun Repeatable.repeat(action: suspend () -> Unit): Unit =
    when {
        isRepeatInfinite -> while (true) {
            action()
            delayNextActionIfNeeded()
        }
        repeatCount > 0 -> repeat(repeatCount) {
            action()
            delayNextActionIfNeeded()
        }
        else -> Unit
    }

private suspend fun Repeatable.delayNextActionIfNeeded() {
    if (this !is RepeatableWithDelay) return
    if (repeatDelayMs == 0L) return

    delay(repeatDelayMs)
}
