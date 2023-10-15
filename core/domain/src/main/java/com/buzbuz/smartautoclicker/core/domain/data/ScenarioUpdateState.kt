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
package com.buzbuz.smartautoclicker.core.domain.data

import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action

internal class ScenarioUpdateState {

    /** Keep track of the events identifiers (DomainId to DatabaseId]. */
    private val eventsDomainToDbIdMap = mutableMapOf<Long, Long>()
    /** Keep track of the conditions identifiers (DomainId to DatabaseId]. */
    private val conditionsDomainToDbIdMap = mutableMapOf<Long, Long>()

    private val eventsToBeRemoved = mutableListOf<EventEntity>()

    fun initUpdateState(oldScenarioEvents: List<EventEntity>) {
        eventsDomainToDbIdMap.clear()
        conditionsDomainToDbIdMap.clear()
        eventsToBeRemoved.apply {
            clear()
            addAll(oldScenarioEvents)
        }
    }

    fun setEventAsKept(eventDbId: Long) {
        eventsToBeRemoved.removeIf { it.id == eventDbId }
    }

    fun getEventToBeRemoved(): List<EventEntity> = eventsToBeRemoved

    fun addEventIdMapping(domainId: Long, dbId: Long) {
        eventsDomainToDbIdMap[domainId] = dbId
    }

    fun getEventDbId(identifier: Identifier?): Long = when {
        identifier != null && identifier.domainId == null && identifier.databaseId != 0L -> identifier.databaseId
        identifier != null -> eventsDomainToDbIdMap[identifier.domainId] ?: throw IllegalStateException("Identifier is not found in event map for $identifier")
        else -> throw IllegalStateException("Event database id can't be found")
    }

    fun getToggleEventDatabaseId(action: Action): Long? =
        if (action is Action.ToggleEvent) {
            val toggleEventId = action.toggleEventId
                ?: throw IllegalArgumentException("Invalid toggle event insertion")
            getEventDbId(toggleEventId)
        } else null

    fun addConditionIdMapping(domainId: Long, dbId: Long) {
        conditionsDomainToDbIdMap[domainId] = dbId
    }

    fun getClickOnConditionDatabaseId(action: Action): Long? =
        if (action is Action.Click) action.clickOnConditionId?.let { getConditionDbId(it) }
        else null

    private fun getConditionDbId(identifier: Identifier?): Long = when {
        identifier != null && identifier.domainId == null && identifier.databaseId != 0L -> identifier.databaseId
        identifier != null -> conditionsDomainToDbIdMap[identifier.domainId]
            ?: throw IllegalStateException("Identifier is not found in condition map for $identifier")
        else -> throw IllegalStateException("Database id can't be found for null condition identifier")
    }
}