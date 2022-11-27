/*
 * Copyright (C) 2022 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.base.bindings.ActionDetails
import com.buzbuz.smartautoclicker.overlays.base.bindings.toActionDetails
import com.buzbuz.smartautoclicker.overlays.base.dialog.CopyViewModel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * View model for the [ActionCopyDialog].
 *
 * @param application the Android application.
 */
class ActionCopyModel(application: Application) : CopyViewModel<Action>(application) {

    /**
     * List of displayed action items.
     * This list can contains all events with headers, or the search result depending on the current search query.
     */
    val actionList: Flow<List<ActionCopyItem>?> =
        combine(repository.getAllActions(), itemsFromCurrentContainer, searchQuery) { dbActions, eventActions, query ->
            eventActions ?: return@combine null
            if (query.isNullOrEmpty()) getAllItems(dbActions, eventActions) else dbActions.toCopyItemsFromSearch(query)
        }

    /**
     * Get a new action based on the provided one.
     * @param action the acton to copy.
     */
    fun getNewActionForCopy(action: Action): Action =
        when (action) {
            is Action.Click -> action.copy(id = 0, name = "" + action.name)
            is Action.Swipe -> action.copy(id = 0, name = "" + action.name)
            is Action.Pause -> action.copy(id = 0, name = "" + action.name)
            is Action.Intent -> action.copy(id = 0, name = "" + action.name)
        }

    /**
     * Get all items with the headers.
     * @param dbActions all actions in the database.
     * @param eventActions all actions in the current event.
     * @return the complete list of action items.
     */
    private fun getAllItems(dbActions: List<Action>, eventActions: List<Action>): List<ActionCopyItem> {
        val allItems = mutableListOf<ActionCopyItem>()

        // First, add the actions from the current event
        val eventItems = eventActions.toCopyItemsFromCurrentEvent()
        if (eventItems.isNotEmpty()) {
            allItems.add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_this))
            allItems.addAll(eventItems)
        }

        // Then, add all other actions. Remove the one already in this event.
        val actions = dbActions.toCopyItemsFromOtherEvents(eventItems)
        if (actions.isNotEmpty()) {
            allItems.add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_all))
            allItems.addAll(actions)
        }

        return allItems
    }

    /**
     * Get the result of the search query.
     * @param query the current search query.
     */
    private fun List<Action>.toCopyItemsFromSearch(query: String) =
        filter { action -> action.name!!.contains(query, true) }
            .map { ActionCopyItem.ActionItem(it.toActionDetails(getApplication())) }
            .distinctByUiDisplay()

    /** */
    private fun List<Action>.toCopyItemsFromCurrentEvent() =
        map { ActionCopyItem.ActionItem(it.toActionDetails(getApplication())) }
            .distinctByUiDisplay()
            .sortedBy { it.actionDetails.name }

    private fun List<Action>.toCopyItemsFromOtherEvents(eventItems: List<ActionCopyItem.ActionItem>) =
        map { ActionCopyItem.ActionItem(it.toActionDetails(getApplication())) }
            .toMutableList()
            .apply {
                removeIf { allItem ->
                    eventItems.find {
                        allItem.actionDetails.action.id == it.actionDetails.action.id
                                || allItem.actionDetails.isSimilar(it.actionDetails)
                    } != null
                }
            }
            .distinctByUiDisplay()

    private fun ActionDetails.isSimilar(other: ActionDetails): Boolean =
        name == other.name && details == other.details && icon == other.icon

    private fun List<ActionCopyItem.ActionItem>.distinctByUiDisplay() =
        distinctBy {
            it.actionDetails.name.hashCode() + it.actionDetails.details.hashCode() + it.actionDetails.icon.hashCode()
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
        data class ActionItem (val actionDetails: ActionDetails) : ActionCopyItem()
    }
}
