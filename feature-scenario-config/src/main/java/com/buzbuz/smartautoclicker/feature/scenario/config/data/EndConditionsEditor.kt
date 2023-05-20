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
package com.buzbuz.smartautoclicker.feature.scenario.config.data

import com.buzbuz.smartautoclicker.domain.model.Identifier
import com.buzbuz.smartautoclicker.domain.model.endcondition.EndCondition
import com.buzbuz.smartautoclicker.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.ListEditor

internal class EndConditionsEditor: ListEditor<EndConditionsEditor.Reference, EndCondition>() {

    override fun createReferenceFromEdition(): Reference =
        Reference(
            scenarioId = getReferenceOrThrow().scenarioId,
            endConditions = editedValue.value ?: emptyList(),
        )

    override fun getValueFromReference(reference: Reference): List<EndCondition> =
        reference.endConditions

    override fun itemMatcher(first: EndCondition, second: EndCondition): Boolean =
        first.id == second.id

    fun createNewItem(): EndCondition =
        EndCondition(
            id = generateNewIdentifier(),
            scenarioId = getReferenceOrThrow().scenarioId,
        )

    fun createNewItemFrom(item: EndCondition): EndCondition =
        item.copy(
            id = generateNewIdentifier(),
            scenarioId = getReferenceOrThrow().scenarioId,
            eventId = item.eventId?.copy(),
            eventName = item.eventName?.let { "" + it },
        )

    fun deleteAllItemsReferencing(event: Event) {
        val newEndConditions = getEditedValueOrThrow()
            .filter { it.eventId != event.id }

        updateEditedValue(newEndConditions)
    }

    internal data class Reference(
        val scenarioId: Identifier,
        val endConditions: List<EndCondition>,
    )
}
