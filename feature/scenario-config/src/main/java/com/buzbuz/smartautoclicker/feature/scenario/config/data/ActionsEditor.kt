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
import com.buzbuz.smartautoclicker.feature.scenario.config.data.base.ListEditor

class ActionsEditor(onListUpdated: (List<Action>) -> Unit): ListEditor<Action>(onListUpdated) {

    val intentExtraEditor = object : ListEditor<IntentExtra<out Any>>(::onEditedActionIntentExtraUpdated, true) {
        override fun areItemsTheSame(a: IntentExtra<out Any>, b: IntentExtra<out Any>): Boolean = a.id == b.id
        override fun isItemComplete(item: IntentExtra<out Any>): Boolean = item.isComplete()
    }

    override fun areItemsTheSame(a: Action, b: Action): Boolean = a.id == b.id
    override fun isItemComplete(item: Action): Boolean = item.isComplete()

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