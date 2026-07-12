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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.action

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.GetActionsForCopyUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ActionsForCopy
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.unreachable.IsActionRelatedToUnreachableItemUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.toUiAction
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.condition.ConditionCopyItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.eventchildren.FixEventChildrenCopyDialog

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.collections.forEach
import kotlin.collections.mapNotNull

/**
 * View model for the [ActionCopyDialog].
 */
class ActionCopyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    getActionsForCopyUseCase: GetActionsForCopyUseCase,
    private val isActionRelatedToUnreachableItemUseCase: IsActionRelatedToUnreachableItemUseCase,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** The currently searched action name. Null if none is. */
    private val searchQuery: MutableStateFlow<String?> = MutableStateFlow(null)

    /** Contains map of action id to the list index checked for copy. */
    private val checkedForCopy: MutableStateFlow<Map<Identifier, ActionWithIndex>> = MutableStateFlow(emptyMap())

    private val actionsToCopy: Flow<ActionsForCopy> = getActionsForCopyUseCase()

    /** List of all actions available for copy */
    private val allCopyItemsState: Flow<ActionCopyUiState> =
        combine(actionsToCopy, checkedForCopy) { actions, checked ->
            ActionCopyUiState(
                items = buildList {
                    if (actions.thisEvent.isNotEmpty()) {
                        add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_this))
                        addAll(actions.thisEvent.toCopyItems(context, checked).sortedBy { it.uiAction.name })
                    }
                    if (actions.thisScenario.isNotEmpty()) {
                        add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_this_scenario))
                        addAll(actions.thisScenario.toCopyItems(context, checked).sortedBy { it.uiAction.name })
                    }
                    if (actions.otherScenario.isNotEmpty()) {
                        add(ActionCopyItem.HeaderItem(R.string.list_header_copy_action_all))
                        addAll(actions.otherScenario.toCopyItems(context, checked).sortedBy { it.uiAction.name })
                    }
                },
                thisEventSize = if (actions.thisEvent.isNotEmpty()) actions.thisEvent.size + 1 else 0,
                thisScenarioSize = if (actions.thisScenario.isNotEmpty()) actions.thisScenario.size + 1 else 0,
                otherScenarioSize = if (actions.otherScenario.isNotEmpty()) actions.otherScenario.size + 1 else 0,
            )
        }

    /**
     * List of displayed action items.
     * This list can contain all events with headers, or the search result depending on the current search query.
     */
    val uiState: StateFlow<ActionCopyUiState?> = allCopyItemsState
        .combine(searchQuery) { state, query ->
            if (query.isNullOrEmpty()) state
            else state.copy(
                items = state.items
                    .filterIsInstance<ActionCopyItem.ActionItem>()
                    .filter { item -> item.uiAction.name.contains(query, true) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    fun toggleCheckedForCopy(action: Action, index: Int) {
        checkedForCopy.update { old ->
            if (old.contains(action.id)) old - action.id
            else old + (action.id to ActionWithIndex(action, index))
        }
    }

    fun getActionsCopy(): List<Action> =
        uiState.value?.items?.mapNotNull { item ->
            if (item !is ActionCopyItem.ActionItem || !item.checked) return@mapNotNull null
            item.uiAction.action.createCopy()
        } ?: emptyList()

    fun actionCopyShouldWarnUser(copyActions: List<Action>): Boolean {
        copyActions.forEach { action ->
            if (isActionRelatedToUnreachableItemUseCase(action)) return true
        }

        return false
    }

    fun getFixEventDialogArgument(actionsToCopy: List<Action>): FixEventChildrenCopyDialog.Arguments? {
        val editedEvent = editionRepository.editionState.getEditedEvent() ?: return null

        val allEvents = editionRepository.editionState.getAllEditedEvents()
        val editedEventIndex = allEvents.indexOfFirst { event -> editedEvent.id == event.id }
        if (editedEventIndex !in allEvents.indices) return null

        val newEvent = editedEvent.copyBase(
            actions = editedEvent.actions + actionsToCopy
        )
        val resultingEventList = allEvents.toMutableList().apply {
            set(editedEventIndex, newEvent)
        }

        return FixEventChildrenCopyDialog.Arguments(
            resultingEventList = resultingEventList,
            parent = newEvent,
            showHelpMessage = true,
        )
    }

    fun saveCopyActions(actionCopies: List<Action>) {
        actionCopies.forEach { action ->
            editionRepository.startActionEdition(action)
            editionRepository.upsertEditedAction()
        }
    }

    private fun Action.createCopy(): Action =
        editionRepository.editedItemsBuilder.createNewActionFrom(this)

    /** Creates copy items from a list of edited actions from this scenario. */
    private fun List<Action>.toCopyItems(context: Context, checked: Map<Identifier, ActionWithIndex>) = map { action ->
        ActionCopyItem.ActionItem(
            uiAction = action.toUiAction(context),
            checked = checked.contains(action.id),
        )
    }

    private data class ActionWithIndex(
        val action: Action,
        val index: Int,
    )
}