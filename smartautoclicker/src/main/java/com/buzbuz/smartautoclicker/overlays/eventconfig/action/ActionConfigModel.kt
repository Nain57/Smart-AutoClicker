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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.click.ClickConfigModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.intent.IntentConfigModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.pause.PauseConfigModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.swipe.SwipeConfigModel
import com.buzbuz.smartautoclicker.overlays.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.overlays.utils.DialogChoice

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * View model for the [ActionConfigDialog].
 *
 * As an [Action] can have multiple types, the values flows representing the configured action will all be different.
 * To achieve this genericity, all action values are contained in a [ActionModel], provided by [actionModel].
 *
 * @param context the Android context.
 */
@OptIn(FlowPreview::class)
class ActionConfigModel(context: Context) : OverlayViewModel(context) {

    /** The action being configured by the user. Defined using [setConfigAction]. */
    private val configuredAction = MutableStateFlow<Action?>(null)
    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = context.getEventConfigPreferences()

    /** The name of the action. */
    val name: Flow<String?> = configuredAction
        .map { it?.name }
        .take(1)

    /** The model for the [configuredAction]. Type will change according to the action type. */
    val actionModel: StateFlow<ActionModel?> = configuredAction
        .map { action ->
            @Suppress("UNCHECKED_CAST") // Nullity is handled first
            when (action) {
                null -> null
                is Action.Click -> ClickConfigModel(viewModelScope, configuredAction)
                is Action.Swipe -> SwipeConfigModel(viewModelScope, configuredAction)
                is Action.Pause -> PauseConfigModel(viewModelScope, configuredAction)
                is Action.Intent -> IntentConfigModel(viewModelScope, configuredAction, context.packageManager)
            }
        }
        .take(1)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null
        )

    /** Tells if the configured action is valid and can be saved. */
    val isValidAction: Flow<Boolean> = actionModel.flatMapConcat { it?.isValidAction ?: flow { emit(false) }}

    /**
     * Set the configured action.
     * This will update all values represented by this view model.
     *
     * @param action the action to configure.
     */
    fun setConfigAction(action: Action) {
        viewModelScope.launch {
            configuredAction.emit(action.deepCopy())
        }
    }

    /**
     * Set the name of the click.
     * @param name the new name.
     */
    fun setName(name: String) {
        viewModelScope.launch {
            when (val action = configuredAction.value) {
                is Action.Click ->  configuredAction.emit(action.copy(name = "" + name))
                is Action.Swipe ->  configuredAction.emit(action.copy(name = "" + name))
                is Action.Pause ->  configuredAction.emit(action.copy(name = "" + name))
                is Action.Intent ->  configuredAction.emit(action.copy(name = "" + name))
                else -> Log.w(TAG, "Can't set name, invalid action type $action")
            }
        }
    }

    /** Save the configured values to restore them at next creation. */
    fun saveLastConfig() {
        actionModel.value?.saveLastConfig(sharedPreferences)
            ?: Log.w(TAG, "Can't save last config, invalid action type")
    }

    /** @return the action currently configured. */
    fun getConfiguredAction(): Action = configuredAction.value!!
}

/** Base class for observing/editing the values for an action. */
abstract class ActionModel {

    /** True if the action values are correct, false if not. */
    abstract val isValidAction: Flow<Boolean>

    /**
     * Save the configured values to restore them at next creation.
     * @param eventConfigPrefs the shared preferences for the event configuration.
     */
    abstract fun saveLastConfig(eventConfigPrefs: SharedPreferences)
}

/** Choices for the target of the click. */
sealed class ClickTargetChoice(title: Int): DialogChoice(title, null) {
    /** Click on the detected condition. */
    object OnCondition : ClickTargetChoice(R.string.dialog_action_config_click_position_on_condition)
    /** Click at a specific location. */
    object AtPosition : ClickTargetChoice(R.string.dialog_action_config_click_position_select_position)
}

/** Check if this duration value is valid for an action. */
fun Long?.isValidDuration(): Boolean = this != null && this > 0

/** Tag for the logs. */
private const val TAG = "ActionConfigModel"
