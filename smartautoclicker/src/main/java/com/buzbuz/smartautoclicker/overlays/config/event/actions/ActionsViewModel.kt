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
package com.buzbuz.smartautoclicker.overlays.config.event.actions

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.extensions.mapList
import com.buzbuz.smartautoclicker.overlays.base.dialog.DialogChoice
import com.buzbuz.smartautoclicker.overlays.base.bindings.ActionDetails
import com.buzbuz.smartautoclicker.overlays.base.bindings.toActionDetails
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultClick
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultIntent
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultPause
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultSwipe

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ActionsViewModel(application: Application) : AndroidViewModel(application) {

    /** The repository of the application. */
    private val repository: Repository = Repository.getRepository(application)
    /** The event currently configured. */
    private lateinit var configuredEvent: MutableStateFlow<Event?>

    /** Tells if there is at least one action to copy. */
    val canCopyAction: Flow<Boolean> = repository.getAllActions()
        .map { it.isNotEmpty() }

    /** List of [actions]. */
    val actions: StateFlow<List<Action>> by lazy {
        configuredEvent
            .map { it?.actions ?: emptyList() }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                emptyList()
            )
    }
    /** List of action details. */
    val actionDetails: StateFlow<List<ActionDetails>> by lazy {
        actions
            .mapList { action -> action.toActionDetails(application) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                emptyList()
            )
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

    fun addUpdateAction(action: Action, index: Int) {
        if (index != -1)  updateAction(action, index)
        else addAction(action)
    }

    /**
     * Add a new action to the event.
     * @param action the new action.
     */
    private fun addAction(action: Action) {
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
    private fun updateAction(action: Action, index: Int) {
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
    fun updateActionOrder(actions: List<ActionDetails>) {
        configuredEvent.value?.let { event ->
            viewModelScope.launch {
                configuredEvent.value = event.copy(actions = actions.map { it.action }.toMutableList())
            }
        }
    }
}

/** Choices for the action type selection dialog. */
sealed class ActionTypeChoice(title: Int, description: Int, iconId: Int?): DialogChoice(title, description, iconId) {
    /** Click Action choice. */
    object Click : ActionTypeChoice(
        R.string.dialog_action_type_click,
        R.string.dialog_desc_click,
        R.drawable.ic_click,
    )
    /** Swipe Action choice. */
    object Swipe : ActionTypeChoice(
        R.string.dialog_action_type_swipe,
        R.string.dialog_desc_swipe,
        R.drawable.ic_swipe,
    )
    /** Pause Action choice. */
    object Pause : ActionTypeChoice(
        R.string.dialog_action_type_pause,
        R.string.dialog_desc_pause,
        R.drawable.ic_wait,
    )
    /** Intent Action choice. */
    object Intent : ActionTypeChoice(
        R.string.dialog_action_type_intent,
        R.string.dialog_desc_intent,
        R.drawable.ic_intent,
    )
}