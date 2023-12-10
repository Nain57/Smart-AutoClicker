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

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.ListEditor

import kotlinx.coroutines.flow.StateFlow

class ActionsEditor(
    onListUpdated: (List<Action>) -> Unit,
    parentItem: StateFlow<Event?>,
): ListEditor<Action, Event>(onListUpdated, parentItem = parentItem) {

    val intentExtraEditor: ListEditor<IntentExtra<out Any>, Action> = ListEditor(
        onListUpdated = ::onEditedActionIntentExtraUpdated,
        canBeEmpty = true,
        parentItem = editedItem,
    )

    override fun startItemEdition(item: Action) {
        super.startItemEdition(item)
        if (item is Action.Intent) {
            intentExtraEditor.startEdition(item.extras ?: emptyList())
        }
    }

    override fun stopItemEdition() {
        intentExtraEditor.stopEdition()
        super.stopItemEdition()
    }

    private fun onEditedActionIntentExtraUpdated(extras: List<IntentExtra<out Any>>) {
        val action = editedItem.value
        if (action == null || action !is Action.Intent) return

        updateEditedItem(action.copy(extras = extras))
    }
}