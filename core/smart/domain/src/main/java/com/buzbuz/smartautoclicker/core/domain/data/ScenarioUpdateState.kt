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
package com.buzbuz.smartautoclicker.core.domain.data

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action

internal class ScenarioUpdateState {

    /** Keep track of the events identifiers (DomainId to DatabaseId]. */
    private val eventsDomainToDbIdMap = mutableMapOf<Long, Long>()
    /** Keep track of the conditions identifiers (DomainId to DatabaseId]. */
    private val conditionsDomainToDbIdMap = mutableMapOf<Long, Long>()
    /** Keep track of the actions identifiers (DomainId to DatabaseId]. */
    private val actionsDomainToDbIdMap = mutableMapOf<Long, Long>()

    fun initUpdateState() {
        eventsDomainToDbIdMap.clear()
        conditionsDomainToDbIdMap.clear()
        actionsDomainToDbIdMap.clear()
    }

    fun addEventIdMapping(domainId: Long, dbId: Long) {
        eventsDomainToDbIdMap[domainId] = dbId
    }

    fun getEventDbId(identifier: Identifier?): Long = when {
        identifier != null && identifier.tempId == null && identifier.databaseId != 0L -> identifier.databaseId
        identifier != null -> eventsDomainToDbIdMap[identifier.tempId] ?: throw IllegalStateException("Identifier is not found in event map for $identifier")
        else -> throw IllegalStateException("Event database id can't be found")
    }

    fun addConditionIdMapping(domainId: Long, dbId: Long) {
        conditionsDomainToDbIdMap[domainId] = dbId
    }

    fun getClickOnConditionDatabaseId(action: Action): Long? =
        if (action is Action.Click) action.clickOnConditionId?.let { getConditionDbId(it) }
        else null

    private fun getConditionDbId(identifier: Identifier?): Long = when {
        identifier != null && identifier.tempId == null && identifier.databaseId != 0L -> identifier.databaseId
        identifier != null -> conditionsDomainToDbIdMap[identifier.tempId]
            ?: throw IllegalStateException("Identifier is not found in condition map for $identifier")
        else -> throw IllegalStateException("Database id can't be found for null condition identifier")
    }

    fun addActionIdMapping(domainId: Long, dbId: Long) {
        actionsDomainToDbIdMap[domainId] = dbId
    }

    fun getActionDbId(identifier: Identifier?): Long = when {
        identifier != null && identifier.tempId == null && identifier.databaseId != 0L -> identifier.databaseId
        identifier != null -> actionsDomainToDbIdMap[identifier.tempId]
            ?: throw IllegalStateException("Identifier is not found in action map for $identifier")
        else -> throw IllegalStateException("Database id can't be found for null action identifier")
    }
}