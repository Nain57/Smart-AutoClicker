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
package com.buzbuz.smartautoclicker.overlays.config.action.copy

import android.app.Application

import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.edition.EditedAction
import com.buzbuz.smartautoclicker.domain.edition.EditionRepository
import com.buzbuz.smartautoclicker.domain.edition.INVALID_EDITED_ITEM_ID
import com.buzbuz.smartautoclicker.overlays.base.bindings.ActionDetails
import com.buzbuz.smartautoclicker.overlays.base.bindings.toActionDetails

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * View model for the [ActionCopyDialog].
 *
 * @param application the Android application.
 */
class ActionCopyModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application)
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    /** List of all actions available for copy */
    private val allCopyItems: Flow<List<ActionCopyItem>> = combine(
        editionRepository.editedEvents,
        editionRepository.editedEvent,
        repository.getAllActions(),
    ) { editedEvents, currentEditedEvent, dbActions ->
        currentEditedEvent ?: return@combine emptyList()

        buildList {
            // First, add the actions from the current event
            val eventItems = currentEditedEvent.editedActions
                .toCopyItemsFromEditedEvents()
                .sortedBy { it.actionDetails.name }
            if (eventItems.isNotEmpty()) {
                add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_this))
                addAll(eventItems)
            }

            // Then, add all other actions. Remove the one already in this event.
            // There should be no ToggleEvent action, as we can't reference an event from another scenario.
            val allOtherActions = buildList {
                val otherEventsActions = buildList {
                    editedEvents
                        .filter { otherEvent -> currentEditedEvent.itemId != otherEvent.itemId }
                        .forEach { otherEditedEvent ->
                            addAll(otherEditedEvent.editedActions.toCopyItemsFromEditedEvents())
                        }
                }

                addAll(otherEventsActions)
                addAll(dbActions.filter { action ->
                    action !is Action.ToggleEvent
                            && eventItems.doesNotContainAction(action.id)
                            && otherEventsActions.doesNotContainAction(action.id)
                }.toCopyItemsFromOtherScenarios())
            }.sortedBy { it.actionDetails.name }

            if (allOtherActions.isNotEmpty()) {
                add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_all))
                addAll(allOtherActions)
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

    /**
     * Get a new action based on the provided one.
     * @param item the item containing the action to copy.
     */
    fun getNewActionForCopy(item: ActionCopyItem.ActionItem): EditedAction =
        editionRepository.createNewActionCopy(item.actionDetails.action, item.toggleEventItemId)

    /** Creates copy items from a list of edited actions from this scenario. */
    private fun List<EditedAction>.toCopyItemsFromEditedEvents() = map {
        ActionCopyItem.ActionItem(
            actionDetails = it.action.toActionDetails(getApplication()),
            toggleEventItemId = if (it.action is Action.ToggleEvent) it.toggleEventItemId else INVALID_EDITED_ITEM_ID,
        )
    }

    /** Creates copy items from a list of actions from another scenario. */
    private fun List<Action>.toCopyItemsFromOtherScenarios() = map {
        ActionCopyItem.ActionItem(
            actionDetails = it.toActionDetails(getApplication()),
        )
    }

    /** Check if this list does not already contains the provided action */
    private fun List<ActionCopyItem.ActionItem>.doesNotContainAction(actionId: Long) = find { item ->
        item.actionDetails.action.id == actionId
    } == null

    /** Remove all identical items from the list. */
    private fun List<ActionCopyItem>.distinctByUiDisplay() =
        distinctBy { item ->
            when (item) {
                is ActionCopyItem.HeaderItem -> item.title.hashCode()
                is ActionCopyItem.ActionItem -> item.actionDetails.name.hashCode() +
                        item.actionDetails.details.hashCode() +
                        item.actionDetails.icon.hashCode() +
                        item.toggleEventItemId.hashCode()
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
         * @param toggleEventItemId the identifier of the event referenced by the toggle event action.
         *                          Null if if not a toggle event.
         */
        data class ActionItem(
            val actionDetails: ActionDetails,
            val toggleEventItemId: Int = INVALID_EDITED_ITEM_ID,
        ) : ActionCopyItem()
    }
}