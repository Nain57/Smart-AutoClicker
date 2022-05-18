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
package com.buzbuz.smartautoclicker.overlays.eventconfig

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.AND
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.OR
import com.buzbuz.smartautoclicker.overlays.utils.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View model for the [EventConfigDialog].
 *
 * @param context the Android context.
 */
@OptIn(FlowPreview::class, ExperimentalStdlibApi::class)
class EventConfigModel(context: Context) : OverlayViewModel(context) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(context)
    /** The event being configured by the user. Defined using [setConfigEvent]. */
    private val configuredEvent = MutableStateFlow<Event?>(null)

    /** Backing property for [actions]. */
    private val _action = configuredEvent
        .map { it?.actions }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )
    /** The event actions currently edited by the user. */
    val actions: StateFlow<List<Action>?> = _action
    /** The item to be displayed in the action list. Last item is always the add actions . */
    val actionListItems: Flow<List<ActionListItem>> = _action
        .combine(repository.getActionsCount()) { actions, actionsCount ->
            buildList {
                actions?.let { actionList ->
                    addAll(actionList.map { ActionListItem.ActionItem(it) })
                }
                add(ActionListItem.AddActionItem(actionsCount > 0))
            }
        }
    /** Backing property for [conditions]. */
    private val _conditions = configuredEvent
        .map { it?.conditions }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )
    /** The event conditions currently edited by the user. */
    val conditions: StateFlow<List<Condition>?> = _conditions
    /** The item to be displayed in the condition list. Last item is always the add conditions. */
    val conditionListItems: Flow<List<ConditionListItem>> = _conditions
        .combine(repository.getConditionsCount()) { conditions, conditionsCount ->
            buildList {
                conditions?.let { conditionList ->
                    addAll(conditionList.map { ConditionListItem.ConditionItem(it) })
                }
                add(ConditionListItem.AddConditionItem(conditionsCount > 0))
            }
        }

    /** The event name value currently edited by the user. */
    val eventName: Flow<String?> = configuredEvent.map { it?.name }.take(1)
    /** The event condition operator currently edited by the user. */
    val conditionOperator: Flow<Int?> = configuredEvent.map { it?.conditionOperator }
    /** Tells if the configured event is valid and can be saved. */
    val isValidEvent: Flow<Boolean> = configuredEvent.map { event ->
        event != null && event.name.isNotEmpty() && !event.actions.isNullOrEmpty() && !event.conditions.isNullOrEmpty()
    }

    /**
     * Set the configured event.
     * This will update all values represented by this view model.
     *
     * @param event the event to configure.
     */
    fun setConfigEvent(event: Event) {
        viewModelScope.launch {
            configuredEvent.value = event.deepCopy()
        }
    }

    /**
     * Get the event with all user changes.
     * @return the event containing all user changes.
     */
    fun getConfiguredEvent(): Event =
        configuredEvent.value ?: throw IllegalStateException("Can't get the configured event, none were defined.")

    /**
     * Set the configured event name.
     * @param name the new event name.
     */
    fun setEventName(name: String) {
        configuredEvent.value?.let { event ->
            configuredEvent.value = event.copy(name = name)
        } ?: throw IllegalStateException("Can't set event name, event is null!")
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

    /** Toggle the condition operator between AND and OR. */
    fun toggleConditionOperator() {
        configuredEvent.value?.let { event ->
            configuredEvent.value = event.copy(
                conditionOperator = if (event.conditionOperator == AND) OR else AND
            )
        } ?: throw IllegalStateException("Can't set condition operator, event is null!")
    }

    /**
     * Create a new condition with the default values from configuration.
     *
     * @param context the Android Context.
     * @param area the area of the condition to create.
     * @param bitmap the image for the condition to create.
     */
    fun createCondition(context: Context, area: Rect, bitmap: Bitmap): Condition {
        configuredEvent.value?.let { event ->
            return newDefaultCondition(
                context = context,
                eventId = event.id,
                bitmap = bitmap,
                area = area,
            )
        } ?: throw IllegalStateException("Can't create a condition, event is null!")
    }

    /**
     * Add a new condition to the event.
     * @param condition the new condition.
     */
    fun addCondition(condition: Condition) {
        configuredEvent.value?.let { event ->
            val newConditions = event.conditions?.let { ArrayList(it) } ?: ArrayList()
            newConditions.add(condition)

            viewModelScope.launch {
                configuredEvent.value = event.copy(conditions = newConditions)
            }
        } ?: throw IllegalStateException("Can't add a condition, event is null!")
    }

    /**
     * Update a condition in the event.
     * @param condition the updated condition.
     */
    fun updateCondition(condition: Condition, index: Int) {
        configuredEvent.value?.let { event ->
            val newConditions = event.conditions?.let { ArrayList(it) } ?: ArrayList()
            newConditions[index] = condition

            viewModelScope.launch {
                configuredEvent.value = event.copy(conditions = newConditions)
            }
        } ?: throw IllegalStateException("Can't update a condition, event is null!")
    }

    /**
     * Remove a condition from the event.
     * @param condition the condition to be removed.
     */
    fun removeCondition(condition: Condition) {
        configuredEvent.value?.let { event ->

            val newConditions = event.conditions?.let { ArrayList(it) } ?: ArrayList()
            if (newConditions.remove(condition)) {
                viewModelScope.launch {
                    configuredEvent.value = event.copy(conditions = newConditions)
                }
            }
        }
    }

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: Condition, onBitmapLoaded: (Bitmap?) -> Unit): Job? {
        if (condition.bitmap != null) {
            onBitmapLoaded.invoke(condition.bitmap)
            return null
        }

        if (condition.path != null) {
            return viewModelScope.launch(Dispatchers.IO) {
                val bitmap = repository.getBitmap(condition.path!!, condition.area.width(), condition.area.height())

                if (isActive) {
                    withContext(Dispatchers.Main) {
                        onBitmapLoaded.invoke(bitmap)
                    }
                }
            }
        }

        onBitmapLoaded.invoke(null)
        return null
    }
}

/** Items displayed in the action list. */
sealed class ActionListItem {
    /** The add action item. */
    data class AddActionItem(val shouldDisplayCopy: Boolean) : ActionListItem()
    /** Item representing a created action. */
    data class ActionItem(val action: Action) : ActionListItem()
}

/** Items displayed in the condition list. */
sealed class ConditionListItem {
    /** The add condition item. */
    data class AddConditionItem(val shouldDisplayCopy: Boolean) : ConditionListItem()
    /** Item representing a created condition. */
    data class ConditionItem(val condition: Condition) : ConditionListItem()
}

/** Choices for the action type selection dialog.*/
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
