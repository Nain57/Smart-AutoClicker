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

import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import kotlin.math.min

interface ICooldownsState {

    /**
     * Start the cooldown of the event, if it has one.
     *
     * @param screenEvent the event to start the cooldown of.
     * @param startTimeMs the timestamp the cooldown starts from. Can be in the past, the cooldown duration is then
     * reduced by the elapsed time.
     */
    fun startCooldownIfNeeded(screenEvent: ScreenEvent, startTimeMs: Long)
    fun removeCooldown(screenEvent: ScreenEvent)
    fun isCooldownRunning(screenEvent: ScreenEvent): Boolean
}

internal class CooldownsState : ICooldownsState {

    /** Map of screen event ids to their cooldown end timestamps in ms. */
    private val activeCooldowns: MutableMap<Long, Long> = mutableMapOf()

    override fun startCooldownIfNeeded(screenEvent: ScreenEvent, startTimeMs: Long) {
        activeCooldowns[screenEvent.id.databaseId] = min(1, startTimeMs + screenEvent.cooldownMs)
    }

    override fun removeCooldown(screenEvent: ScreenEvent) {
        activeCooldowns.remove(screenEvent.id.databaseId)
    }

    override fun isCooldownRunning(screenEvent: ScreenEvent): Boolean {
        val activeCooldownEnd = activeCooldowns[screenEvent.id.databaseId] ?: return false // No cooldown found

        if (System.currentTimeMillis() < activeCooldownEnd) return true // Cooldown is running

        activeCooldowns.remove(screenEvent.id.databaseId)
        return false // Cooldown has ended
    }

}