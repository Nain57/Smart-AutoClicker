/*
 * Copyright (C) 2021 Nain57
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
import com.buzbuz.smartautoclicker.database.Repository
import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.database.domain.Condition
import com.buzbuz.smartautoclicker.database.domain.ConditionOperator
import com.buzbuz.smartautoclicker.database.domain.Event
import com.buzbuz.smartautoclicker.overlays.utils.DialogChoice

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View model for the [EventConfigDialog].
 *
 * @param context the Android context.
 */
class EventConfigModel(context: Context) : OverlayViewModel(context) {

    /**
     * Job for updating [configuredEvent] values that depends on an EditText.
     * As the user can stop the EditText edition with a lots of different ways, it is difficult to tell exactly when the
     * user is done editing. As a solution, we listen to each text edition and call the model for an update. But those
     * calls can be numerous and this leads to a slow UI feeling when editing.
     * So we delay those calls using this [Job] by [EDIT_TEXT_UPDATE_DELAY] to only update once after the user have
     * stopped editing for a moment.
     * This Job is null when the user isn't editing.
     */
    private var editNameJob: Job? = null

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(context)
    /** The event being configured by the user. Defined using [setConfigEvent]. */
    private val configuredEvent = MutableStateFlow<Event?>(null)

    /** The event name value currently edited by the user. */
    val eventName: Flow<String?> = configuredEvent.map { it?.name }
    /** The event actions currently edited by the user. */
    val actions: Flow<List<Action>?> = configuredEvent.map { it?.actions }
    /** The event condition operator currently edited by the user. */
    val conditionOperator: Flow<Int?> = configuredEvent.map { it?.conditionOperator }
    /** The event conditions currently edited by the user. */
    val conditions: Flow<List<Condition>?> = configuredEvent.map { it?.conditions }
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

    /** @return the event containing all user changes. */
    fun getConfiguredEvent(): Event =
        configuredEvent.value ?: throw IllegalStateException("Can't get the configured event, none were defined.")

    /**
     * Set the name of the configured event.
     * @param name the new name.
     */
    fun setEventName(name: String) {
        configuredEvent.value?.let { event ->
            editNameJob?.cancel()
            editNameJob = viewModelScope.launch {
                delay(EDIT_TEXT_UPDATE_DELAY)
                configuredEvent.value = event.copy(name = name)
            }
        }
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
                is ActionTypeChoice.Click -> Action.Click(
                    eventId = event.id,
                    name = context.getString(R.string.default_click_name),
                    pressDuration = context.resources.getInteger(R.integer.default_click_press_duration).toLong(),
                )
                is ActionTypeChoice.Swipe -> Action.Swipe(
                    eventId = event.id,
                    name = context.getString(R.string.default_swipe_name),
                    swipeDuration = context.resources.getInteger(R.integer.default_swipe_duration).toLong(),
                )
                is ActionTypeChoice.Pause -> Action.Pause(
                    eventId = event.id,
                    name = context.getString(R.string.default_pause_name),
                    pauseDuration = context.resources.getInteger(R.integer.default_pause_duration).toLong(),
                )
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
     * Set the condition operator of the configured event.
     * @param operator the new operator.
     */
    fun setConditionOperator(@ConditionOperator operator: Int) {
        configuredEvent.value?.let { event ->
            configuredEvent.value = event.copy(conditionOperator = operator)
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
            return Condition(
                eventId = event.id,
                bitmap = bitmap,
                area = area,
                threshold = context.resources.getInteger(R.integer.default_condition_threshold),
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

/**
 * Choices for the condition operator selection dialog.
 *
 * @param title the string res of the title for this choice.
 * @param iconId the icon res of the image for this choice.
 */
sealed class OperatorChoice(title: Int, iconId: Int?): DialogChoice(title, iconId) {
    /** AND choice. */
    object And : OperatorChoice(R.string.condition_operator_and_desc, R.drawable.ic_all_conditions)
    /** OR choice. */
    object Or : OperatorChoice(R.string.condition_operator_or_desc, R.drawable.ic_one_condition)
}

/**
 * Choices for the action type selection dialog.
 *
 * @param title the string res of the title for this choice.
 * @param iconId the icon res of the image for this choice.
 */
sealed class ActionTypeChoice(title: Int, iconId: Int?): DialogChoice(title, iconId) {
    /** Click Action choice. */
    object Click : ActionTypeChoice(R.string.dialog_action_type_click, R.drawable.ic_click)
    /** Swipe Action choice. */
    object Swipe : ActionTypeChoice(R.string.dialog_action_type_swipe, R.drawable.ic_swipe)
    /** Pause Action choice. */
    object Pause : ActionTypeChoice(R.string.dialog_action_type_pause, R.drawable.ic_wait)
}

/** Delay without update before updating the action. */
private const val EDIT_TEXT_UPDATE_DELAY = 750L
