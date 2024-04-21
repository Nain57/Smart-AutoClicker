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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.copy


import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.ActionDetails
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.toActionDetails
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * View model for the [ActionCopyDialog].
 */
class ActionCopyModel @Inject constructor(
    @ApplicationContext context: Context,
    editionRepository: EditionRepository,
) : ViewModel() {

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    /** List of all actions available for copy */
    private val allCopyItems: Flow<List<ActionCopyItem>> =
        combine(
            editionRepository.editionState.editedEventState,
            editionRepository.editionState.actionsForCopy,
        ) { editedEventState, actions ->

            val editedEvent = editedEventState.value ?: return@combine emptyList()
            val editedActions = mutableListOf<Action>()
            val otherActions = mutableListOf<Action>()
            actions.forEach { action ->
                if (editedEvent.id == action.eventId) editedActions.add(action)
                else otherActions.add(action)
            }

            buildList {
                if (editedActions.isNotEmpty()) {
                    add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_this))
                    addAll(editedActions.toCopyItems(context).sortedBy { it.actionDetails.name })
                }
                if (otherActions.isNotEmpty()) {
                    add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_all))
                    addAll(otherActions.toCopyItems(context).sortedBy { it.actionDetails.name })
                }
            }
        }

    /**
     * List of displayed action items.
     * This list can contains all events with headers, or the search result depending on the current search query.
     */
    val actionList: Flow<List<ActionCopyItem>?> = allCopyItems.combine(searchQuery) { allItems, query ->
        if (query.isNullOrEmpty()) allItems
        else allItems
            .filterIsInstance<ActionCopyItem.ActionItem>()
            .filter { item -> item.actionDetails.name.contains(query, true) }
    }

    /**
     * Update the action search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /** Creates copy items from a list of edited actions from this scenario. */
    private fun List<Action>.toCopyItems(context: Context) = map { action ->
        ActionCopyItem.ActionItem(
            actionDetails = action.toActionDetails(context),
        )
    }

    /** Types of items in the action copy list. */
    sealed class ActionCopyItem {

        /**
         * Header item, delimiting sections.
         * @param title the title for the header.
         */
        data class HeaderItem(@StringRes val title: Int) : ActionCopyItem()

        /**
         * Action item.
         * @param actionDetails the details for the action.
         */
        data class ActionItem(
            val actionDetails: ActionDetails,
        ) : ActionCopyItem()
    }
}