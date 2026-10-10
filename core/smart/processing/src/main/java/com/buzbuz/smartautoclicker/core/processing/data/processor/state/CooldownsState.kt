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
package com.buzbuz.smartautoclicker.core.processing.data.processor.state

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import kotlin.math.max

interface ICooldownsState {

    /**
     * Start the cooldown of the event, if it has one.
     *
     * @param event the event to start the cooldown of. Only a [ScreenEvent] has a cooldown of its own.
     * @param startTimeMs the timestamp the cooldown starts from. Can be in the past, the cooldown duration is then
     * reduced by the elapsed time.
     * @param minDurationMs the minimum duration of the cooldown, applied even if the event has no cooldown of its own.
     */
    fun startCooldownIfNeeded(event: Event, startTimeMs: Long, minDurationMs: Long = 0L)
    fun removeCooldown(event: Event)
    fun isCooldownRunning(event: Event): Boolean
}

internal class CooldownsState : ICooldownsState {

    /** Map of event ids to their cooldown end timestamps in ms. */
    private val activeCooldowns: MutableMap<Long, Long> = mutableMapOf()

    override fun startCooldownIfNeeded(event: Event, startTimeMs: Long, minDurationMs: Long) {
        val durationMs = max((event as? ScreenEvent)?.cooldownMs ?: 0L, minDurationMs)
        activeCooldowns[event.id.databaseId] = max(1, startTimeMs + durationMs)
    }

    override fun removeCooldown(event: Event) {
        activeCooldowns.remove(event.id.databaseId)
    }

    override fun isCooldownRunning(event: Event): Boolean {
        val activeCooldownEnd = activeCooldowns[event.id.databaseId] ?: return false // No cooldown found

        if (System.currentTimeMillis() < activeCooldownEnd) return true // Cooldown is running

        activeCooldowns.remove(event.id.databaseId)
        return false // Cooldown has ended
    }

}
