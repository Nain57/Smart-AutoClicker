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

import com.buzbuz.smartautoclicker.core.domain.model.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.Editor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class EventEditor: Editor<EventEditor.Reference, Event>() {

    val conditionsEditor: ConditionsEditor = ConditionsEditor()
    val actionsEditor: ActionsEditor = ActionsEditor()

    val containsChange: Flow<Boolean> = combine(reference, editedValue) { ref, editedEvent ->
        if (ref == null || editedEvent == null) false
        else ref.event != editedEvent
    }

    override fun getValueFromReference(reference: Reference): Event =
        reference.event

    override fun onEditionStarted(reference: Reference) {
        conditionsEditor.startEdition(
            ConditionsEditor.Reference(
                eventId = reference.event.id,
                conditions = reference.event.conditions,
            )
        )

        actionsEditor.startEdition(
            ActionsEditor.Reference(
                eventId = reference.event.id,
                actions = reference.event.actions,
            )
        )
    }

    fun createCondition(context: Context, area: Rect?, bitmap: Bitmap?, from: Condition?) = when {
        area != null && bitmap != null -> conditionsEditor.createNewItem(context, area, bitmap)
        from != null -> conditionsEditor.createNewItemFrom(from)
        else -> throw IllegalArgumentException("Condition should be created from bitmap or copy")
    }

    fun upsertCondition(condition: Condition) {
        conditionsEditor.upsertItem(condition)
        syncConditions()
    }

    fun deleteCondition(condition: Condition) {
        conditionsEditor.deleteItem(condition)
        syncConditions()
    }

    private fun syncConditions() {
        updateEditedValue(
            getEditedValueOrThrow().copy(
                conditions = conditionsEditor.getEditedValueOrThrow().toMutableList(),
            )
        )
    }

    fun createNewClick(context: Context, from: Action.Click?) =
        actionsEditor.createNewClick(context, from)

    fun createNewSwipe(context: Context, from: Action.Swipe?) =
        actionsEditor.createNewSwipe(context, from)

    fun createNewPause(context: Context, from: Action.Pause?) =
        actionsEditor.createNewPause(context, from)

    fun createNewIntent(context: Context, from: Action.Intent?) =
        actionsEditor.createNewIntent(context, from)

    fun createNewIntentExtra(actionId: Identifier) =
        actionsEditor.createNewIntentExtra(actionId)

    fun createNewToggleEvent(context: Context, from: Action.ToggleEvent?) =
        actionsEditor.createNewToggleEvent(context, from)

    fun upsertAction(action: Action) {
        actionsEditor.upsertItem(action)
        syncAction()
    }

    fun deleteAction(action: Action) {
        actionsEditor.deleteItem(action)
        syncAction()
    }

    fun updateActionsOrder(newActions: List<Action>) {
        actionsEditor.updateList(newActions)
        syncAction()
    }

    private fun syncAction() {
        updateEditedValue(
            getEditedValueOrThrow().copy(
                actions = actionsEditor.getEditedValueOrThrow().toMutableList(),
            )
        )
    }

    override fun onEditionFinished(): Reference {
        conditionsEditor.finishEdition()
        actionsEditor.finishEdition()
        return Reference(event = getEditedValueOrThrow())
    }

    internal data class Reference(
        val event: Event,
    )
}