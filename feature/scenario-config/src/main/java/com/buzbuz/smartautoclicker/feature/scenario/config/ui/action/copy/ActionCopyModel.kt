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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.copy

import android.app.Application

import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.ActionDetails
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.toActionDetails

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * View model for the [ActionCopyDialog].
 *
 * @param application the Android application.
 */
class ActionCopyModel(application: Application) : AndroidViewModel(application) {

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    /** List of all actions available for copy */
    private val allCopyItems: Flow<List<ActionCopyItem>> = combine(
        editionRepository.editionState.editedEventState,
        editionRepository.editionState.editedScenarioOtherActionsForCopy,
        editionRepository.editionState.allOtherScenarioActionsForCopy,
    ) { editedEvent, otherActionsFromEditedScenario, otherActionsFromOtherScenario ->
        buildList {
            val editedEvt = editedEvent.value ?: return@combine emptyList()

            // First, add the actions from the current event
            if (editedEvt.actions.isNotEmpty()) {
                add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_this))
                addAll(
                    editedEvt.actions
                        .toCopyItemsFromEditedEvents()
                        .sortedBy { it.actionDetails.name }
                )
            }

            val allOtherActions = mutableListOf<Action>().apply {
                addAll(otherActionsFromEditedScenario)
                addAll(otherActionsFromOtherScenario)
            }
            if (allOtherActions.isNotEmpty()) {
                add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_all))
                addAll(
                    allOtherActions
                        .toCopyItemsFromOtherScenarios()
                        .sortedBy { it.actionDetails.name }
                )
            }
        }.distinctByUiDisplay()
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
            .sortedBy { it.actionDetails.name }
            .distinctByUiDisplay()
    }

    /**
     * Update the action search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /** Creates copy items from a list of edited actions from this scenario. */
    private fun List<Action>.toCopyItemsFromEditedEvents() = map { action ->
        ActionCopyItem.ActionItem(
            actionDetails = action.toActionDetails(getApplication()),
        )
    }

    /** Creates copy items from a list of actions from another scenario. */
    private fun List<Action>.toCopyItemsFromOtherScenarios() = map {
        ActionCopyItem.ActionItem(
            actionDetails = it.toActionDetails(getApplication()),
        )
    }

    /** Remove all identical items from the list. */
    private fun List<ActionCopyItem>.distinctByUiDisplay() =
        distinctBy { item ->
            when (item) {
                is ActionCopyItem.HeaderItem -> item.title.hashCode()
                is ActionCopyItem.ActionItem -> item.actionDetails.name.hashCode() +
                        item.actionDetails.details.hashCode() +
                        item.actionDetails.icon.hashCode() +
                        item.actionDetails.action.hashCode()
            }
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