/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.event.actions

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.overlays.utils.*

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ActionsViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application.applicationContext)

    /** The event currently configured. */
    private lateinit var configuredEvent: MutableStateFlow<Event?>

    /** Backing property for [actions]. */
    private val _action by lazy {
        configuredEvent
            .map { it?.actions }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                emptyList()
            )
    }
    /** The event actions currently edited by the user. */
    val actions: StateFlow<List<Action>?> get() = _action
    /** The item to be displayed in the action list. Last item is always the add actions . */
    val actionListItems: Flow<List<ActionListItem>> by lazy {
        _action.combine(repository.getActionsCount()) { actions, actionsCount ->
            buildList {
                actions?.let { actionList ->
                    addAll(actionList.map { ActionListItem.ActionItem(it) })
                }
                add(ActionListItem.AddActionItem(actionsCount > 0))
            }
        }
    }

    /** Set the event currently configured by the UI. */
    fun setConfiguredEvent(event: MutableStateFlow<Event?>) {
        configuredEvent = event
    }

    /**
     * Create a new action with the default values from configuration.
     *
     * @param context the Android Context.
     * @param actionType the type of action to create.
     */
    fun createAction(context: Context, actionType: ActionTypeChoice): Action {
        configuredEvent.value?.let { event ->

            return when (actionType) {
                is ActionTypeChoice.Click -> newDefaultClick(context, event.id)
                is ActionTypeChoice.Swipe -> newDefaultSwipe(context, event.id)
                is ActionTypeChoice.Pause -> newDefaultPause(context, event.id)
                is ActionTypeChoice.Intent -> newDefaultIntent(context, event.id)
            }

        } ?: throw IllegalStateException("Can't create an action, event is null!")
    }

    /**
     * Add a new action to the event.
     * @param action the new action.
     */
    fun addAction(action: Action) {
        configuredEvent.value?.let { event ->
            val newActions = event.actions?.let { ArrayList(it) } ?: ArrayList()
            newActions.add(action)

            viewModelScope.launch {
                configuredEvent.value = event.copy(actions = newActions)
            }
        }
    }

    /**
     * Update an action in the event.
     * @param action the updated action.
     */
    fun updateAction(action: Action, index: Int) {
        configuredEvent.value?.let { event ->
            val newActions = event.actions?.let { ArrayList(it) } ?: ArrayList()
            newActions[index] = action

            viewModelScope.launch {
                configuredEvent.value = event.copy(actions = newActions)
            }
        }
    }

    /**
     * Remove an action from the event.
     * @param action the action to be removed.
     */
    fun removeAction(action: Action) {
        configuredEvent.value?.let { event ->

            val newActions = event.actions?.let { ArrayList(it) } ?: ArrayList()
            if (newActions.remove(action)) {
                viewModelScope.launch {
                    configuredEvent.value = event.copy(actions = newActions)
                }
            }
        }
    }

    /**
     * Update the priority of the actions.
     * @param actions the new actions order.
     */
    fun updateActionOrder(actions: List<ActionListItem>) {
        configuredEvent.value?.let { event ->
            viewModelScope.launch {
                val newActions = actions.mapNotNull {
                    when (it) {
                        is ActionListItem.AddActionItem -> null
                        is ActionListItem.ActionItem -> it.action
                    }
                }.toMutableList()
                configuredEvent.value = event.copy(actions = newActions)
            }
        }
    }
}

/** Items displayed in the action list. */
sealed class ActionListItem {
    /** The add action item. */
    data class AddActionItem(val shouldDisplayCopy: Boolean) : ActionListItem()
    /** Item representing a created action. */
    data class ActionItem(val action: Action) : ActionListItem()
}

/** Choices for the action type selection dialog. */
sealed class ActionTypeChoice(title: Int, iconId: Int?): DialogChoice(title, iconId) {
    /** Click Action choice. */
    object Click : ActionTypeChoice(R.string.dialog_action_type_click, R.drawable.ic_click)
    /** Swipe Action choice. */
    object Swipe : ActionTypeChoice(R.string.dialog_action_type_swipe, R.drawable.ic_swipe)
    /** Pause Action choice. */
    object Pause : ActionTypeChoice(R.string.dialog_action_type_pause, R.drawable.ic_wait)
    /** Intent Action choice. */
    object Intent : ActionTypeChoice(R.string.dialog_action_type_intent, R.drawable.ic_intent)
}