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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.domain.model.EXACT
import com.buzbuz.smartautoclicker.domain.model.Identifier
import com.buzbuz.smartautoclicker.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.ListEditor

internal class ConditionsEditor : ListEditor<ConditionsEditor.Reference, Condition>() {

    override fun createReferenceFromEdition(): Reference =
        Reference(
            eventId = getReferenceOrThrow().eventId,
            conditions = editedValue.value ?: emptyList(),
        )

    override fun getValueFromReference(reference: Reference): List<Condition> =
        reference.conditions

    override fun itemMatcher(first: Condition, second: Condition): Boolean =
        first.id == second.id

    fun createNewItem(context: Context, area: Rect, bitmap: Bitmap): Condition =
        Condition(
            id = generateNewIdentifier(),
            eventId = getReferenceOrThrow().eventId,
            name = context.resources.getString(R.string.default_condition_name),
            bitmap = bitmap,
            area = area,
            threshold = context.resources.getInteger(R.integer.default_condition_threshold),
            detectionType = EXACT,
            shouldBeDetected = true,
        )

    fun createNewItemFrom(item: Condition): Condition =
        createNewItemFrom(item, getReferenceOrThrow().eventId)

    fun createNewItemsFrom(items: List<Condition>, eventId: Identifier) =
        items.map { condition -> createNewItemFrom(condition, eventId) }

    private fun createNewItemFrom(condition: Condition, eventId: Identifier) =
        condition.copy(
            id = generateNewIdentifier(),
            eventId = eventId,
            name = "" + condition.name,
            path = if (condition.path != null) "" + condition.path else null,
        )

    internal data class Reference(
        val eventId: Identifier,
        val conditions: List<Condition>,
    )
}