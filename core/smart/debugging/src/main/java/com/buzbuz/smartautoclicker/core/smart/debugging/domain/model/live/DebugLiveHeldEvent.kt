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
package com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.live

import com.buzbuz.smartautoclicker.core.domain.model.event.Event

/**
 * Event holding a pointer on the screen during a live debugging session.
 *
 * @param event the event holding the pointer.
 * @param holdDurationS the duration since the pointer is held, in seconds.
 */
data class DebugLiveHeldEvent(
    val event: Event,
    val holdDurationS: Long,
)
