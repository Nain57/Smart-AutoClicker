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
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.util.Log

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.overlays.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.overlays.utils.putClickPressDurationConfig
import com.buzbuz.smartautoclicker.overlays.utils.putPauseDurationConfig
import com.buzbuz.smartautoclicker.overlays.utils.putSwipeDurationConfig
import com.buzbuz.smartautoclicker.overlays.utils.DialogChoice

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.lang.UnsupportedOperationException

/**
 * View model for the [ActionConfigDialog].
 *
 * As an [Action] can have multiple types, the values flows representing the configured action will all be different.
 * To achieve this genericity, all action values are contained in a [ActionValues], provided by [actionValues].
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

    /** Tells if the configured action is valid and can be saved. */
    val isValidAction: Flow<Boolean> = configuredAction.map { action ->
        when (action) {
            null -> false
            is Action.Click -> !action.name.isNullOrEmpty() && ((action.x != null && action.y != null) || action.clickOnCondition)
                    && action.pressDuration.isValidDuration()

            is Action.Swipe -> !action.name.isNullOrEmpty() && action.fromX != null && action.fromY != null
                    && action.toX != null && action.toY != null && action.swipeDuration.isValidDuration()

            is Action.Pause -> !action.name.isNullOrEmpty() && action.pauseDuration.isValidDuration()

            is Action.Intent -> !action.name.isNullOrEmpty() && action.isAdvanced != null && action.intentAction != null
                    && action.flags != null
        }
    }

    /** The values for the [configuredAction]. Type will change according to the action type. */
    val actionValues = configuredAction
        .map { action ->
            @Suppress("UNCHECKED_CAST") // Nullity is handled first
            when (action) {
                null -> null
                is Action.Click -> ClickActionValues()
                is Action.Swipe -> SwipeActionValues()
                is Action.Pause -> PauseActionValues()
                is Action.Intent -> IntentActionValues()
            }
        }
        .take(1)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null
        )

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
                else -> Log.w(TAG, "Can't set name, invalid action type $action")
            }
        }
    }

    /** Save the configured values to restore them at next creation. */
    fun saveLastConfig() {
        when (val action = configuredAction.value) {
            is Action.Click ->
                sharedPreferences.edit().putClickPressDurationConfig(action.pressDuration ?: 0).apply()
            is Action.Swipe ->
                sharedPreferences.edit().putSwipeDurationConfig(action.swipeDuration ?: 0).apply()
            is Action.Pause ->
                sharedPreferences.edit().putPauseDurationConfig(action.pauseDuration ?: 0).apply()
            is Action.Intent -> throw UnsupportedOperationException()
            null -> Log.w(TAG, "Can't save last config, invalid action type $action")
        }
    }

    /** @return the action currently configured. */
    fun getConfiguredAction(): Action = configuredAction.value!!

    /** Base class for observing/editing the values for an action. */
    abstract inner class ActionValues

    /** Allow to observe/edit the value a click action. */
    inner class ClickActionValues : ActionValues() {

        /** The duration between the press and release of the click in milliseconds. */
        val pressDuration: Flow<Long?> = configuredAction
            .map { (it as Action.Click).pressDuration }
            .take(1)
        /** The position of the click. */
        val position: Flow<Point?> = configuredAction.map { click ->
            if (click is Action.Click && click.x != null && click.y != null) {
                Point(click.x!!, click.y!!)
            } else {
                null
            }
        }
        /** If the click should be made on the detected condition. */
        val clickOnCondition: Flow<Boolean> = configuredAction.map { click ->
            click is Action.Click && click.clickOnCondition
        }

        /**
         * Set if this click should be made on the detected condition.
         * @param enabled true to click on the detected condition, false to let the user pick its own location.
         */
        fun setClickOnCondition(enabled: Boolean) {
            (configuredAction.value as Action.Click).let { click ->
                viewModelScope.launch {
                    configuredAction.emit(click.copy(clickOnCondition = enabled))
                }
            }
        }

        /**
         * Set the position of the click.
         * @param position the new position.
         */
        fun setPosition(position: Point) {
            (configuredAction.value as Action.Click).let { click ->
                viewModelScope.launch {
                    configuredAction.emit(click.copy(x = position.x, y = position.y))
                }
            }
        }

        /**
         * Set the press duration of the click.
         * @param durationMs the new duration in milliseconds.
         */
        fun setPressDuration(durationMs: Long?) {
            (configuredAction.value as Action.Click).let { click ->
                viewModelScope.launch {
                    configuredAction.emit(click.copy(pressDuration = durationMs))
                }
            }
        }
    }

    /** Allow to observe/edit the value a swipe action. */
    inner class SwipeActionValues : ActionValues() {

        /** The duration between the start and end of the swipe in milliseconds. */
        val swipeDuration: Flow<Long?> = configuredAction
            .map { (it as Action.Swipe).swipeDuration }
            .take(1)
        /** The start and end positions of the swipe. */
        val positions: Flow<Pair<Point?, Point?>> = configuredAction.map { swipe ->
            if (swipe is Action.Swipe && swipe.fromX != null && swipe.fromY != null
                && swipe.toX != null && swipe.toY != null) {

                Point(swipe.fromX!!, swipe.fromY!!) to Point(swipe.toX!!, swipe.toY!!)
            } else {
                null to null
            }
        }

        /**
         * Set the start and end positions of the swipe.
         * @param from the new start position.
         * @param to the new end position.
         */
        fun setPositions(from: Point, to: Point) {
            (configuredAction.value as Action.Swipe).let { swipe ->
                viewModelScope.launch {
                    configuredAction.emit(swipe.copy(fromX = from.x, fromY = from.y, toX = to.x, toY = to.y))
                }
            }
        }

        /**
         * Set the duration of the swipe.
         * @param durationMs the new duration in milliseconds.
         */
        fun setSwipeDuration(durationMs: Long?) {
            (configuredAction.value as Action.Swipe).let { swipe ->
                viewModelScope.launch {
                    configuredAction.emit(swipe.copy(swipeDuration = durationMs))
                }
            }
        }
    }

    /** Allow to observe/edit the value a pause action. */
    inner class PauseActionValues : ActionValues() {

        /** The duration of the pause in milliseconds. */
        val pauseDuration: Flow<Long?> = configuredAction
            .map { (it as Action.Pause).pauseDuration }
            .take(1)

        /**
         * Set the duration of the pause.
         * @param durationMs the new duration in milliseconds.
         */
        fun setPauseDuration(durationMs: Long?) {
            (configuredAction.value as Action.Pause).let { pause ->
                viewModelScope.launch {
                    configuredAction.emit(pause.copy(pauseDuration = durationMs))
                }
            }
        }
    }

    /** Allow to observe/edit the value an intent action. */
    inner class IntentActionValues : ActionValues() {

        /**  */
        val isAdvanced: Flow<Boolean> = configuredAction.map { intent ->
            intent is Action.Intent && intent.isAdvanced ?: false
        }

        /** */
        fun toggleIsAdvanced() {
            (configuredAction.value as Action.Intent).let { intent ->
                viewModelScope.launch {
                    configuredAction.emit(intent.copy(isAdvanced = !(intent.isAdvanced ?: false)))
                }
            }
        }
    }
}

/** Choices for the target of the click. */
sealed class ClickTargetChoice(title: Int): DialogChoice(title, null) {
    /** Click on the detected condition. */
    object OnCondition : ClickTargetChoice(R.string.dialog_action_config_click_position_on_condition)
    /** Click at a specific location. */
    object AtPosition : ClickTargetChoice(R.string.dialog_action_config_click_position_select_position)
}

/** Tag for the logs. */
private const val TAG = "ActionConfigModel"
/** Check if this duration value is valid for an action. */
private fun Long?.isValidDuration(): Boolean = this != null && this > 0