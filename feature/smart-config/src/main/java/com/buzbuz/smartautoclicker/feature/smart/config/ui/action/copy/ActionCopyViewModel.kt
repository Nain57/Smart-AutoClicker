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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.actions.text.findCounterReferences
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.GetActionsForCopyUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ActionsForCopy
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.toUiAction
import com.buzbuz.smartautoclicker.feature.smart.config.utils.isClickOnCondition

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.collections.mapNotNull

/**
 * View model for the [ActionCopyDialog].
 */
class ActionCopyViewModel @Inject constructor(
    @ApplicationContext context: Context,
    getActionsForCopyUseCase: GetActionsForCopyUseCase,
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


    fun toggleCheckedForCopy(action: Action, index: Int) {
        checkedForCopy.update { old ->
            if (old.contains(action.id)) old - action.id
            else old + (action.id to ActionWithIndex(action, index))
        }
    }

    fun actionCopyShouldWarnUser(): Boolean {
        val state = uiState.value ?: return false

        // If there is only one action that references something unreachable, we should warn.
        // So return early we one is found.
        checkedForCopy.value.forEach { (_, actionWithIndex) ->

            // It's always safe to copy from the same event
            if (state.thisEventSize > 0 && actionWithIndex.index in 0 until state.thisEventSize) return@forEach

            // In the same scenario but different event
            val thisScenarioStartIndex = state.thisEventSize
            val otherScenarioStartIndex = thisScenarioStartIndex + state.thisScenarioSize
            if (state.thisScenarioSize > 0 && actionWithIndex.index in thisScenarioStartIndex until otherScenarioStartIndex) {
                if (actionWithIndex.action.isRelatedToItsEvent()) return true
                return@forEach
            }

            // In the same scenario but different event
            val otherScenarioEndIndex = otherScenarioStartIndex + state.otherScenarioSize - 1
            if (state.otherScenarioSize > 0 && actionWithIndex.index in otherScenarioStartIndex .. otherScenarioEndIndex) {
                if (actionWithIndex.action.isRelatedToItsScenario()) return true
                return@forEach
            }
        }

        return false
    }

    fun getActionsToCopy(): List<Action>  = checkedForCopy.value
        .mapNotNull { (_, actionWithIndex) -> actionWithIndex.action }

    /**
     * Update the action search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /** Creates copy items from a list of edited actions from this scenario. */
    private fun List<Action>.toCopyItems(context: Context, checked: Map<Identifier, ActionWithIndex>) = map { action ->
        ActionCopyItem.ActionItem(
            uiAction = action.toUiAction(context),
            checked = checked.contains(action.id),
        )
    }

    private fun Action.isRelatedToItsEvent(): Boolean =
        when (this) {
            // Condition can't be reached in another event
            is Click -> isClickOnCondition()

            // Same scenario, so events & counters are always reachable
            is ChangeCounter,
            is Intent,
            is Notification,
            is Pause,
            is SetText,
            is Swipe,
            is SystemAction,
            is ToggleEvent -> return false
        }

    private fun Action.isRelatedToItsScenario(): Boolean =
        when (this) {
            is Click -> isClickOnCondition()

            // Other scenario, so events & counters never reachable
            is ChangeCounter,
            is ToggleEvent -> return true

            // Nothing referenced besides counters, so they are always reachable as well
            is Notification -> messageText.findCounterReferences().isNotEmpty()
            is SetText -> text.findCounterReferences().isNotEmpty()

            // Nothing is reference in those actions
            is Pause,
            is Swipe,
            is Intent,
            is SystemAction -> return false
        }

    private data class ActionWithIndex(
        val action: Action,
        val index: Int,
    )
}