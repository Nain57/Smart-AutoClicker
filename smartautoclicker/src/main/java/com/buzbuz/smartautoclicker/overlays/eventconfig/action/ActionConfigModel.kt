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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.overlays.eventconfig.EVENT_CONFIG_PREFERENCES_NAME
import com.buzbuz.smartautoclicker.overlays.eventconfig.getClickPressDurationConfig
import com.buzbuz.smartautoclicker.overlays.eventconfig.getPauseDurationConfig
import com.buzbuz.smartautoclicker.overlays.eventconfig.getSwipeDurationConfig
import com.buzbuz.smartautoclicker.overlays.eventconfig.putClickPressDurationConfig
import com.buzbuz.smartautoclicker.overlays.eventconfig.putPauseDurationConfig
import com.buzbuz.smartautoclicker.overlays.eventconfig.putSwipeDurationConfig

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * View model for the [ActionConfigDialog].
 *
 * As an [Action] can have multiple types, the values flows representing the configured action will all be different.
 * To achieve this genericity, all action values are contained in a [ActionValues], provided by [actionValues].
 *
 * @param context the Android context.
 */
class ActionConfigModel(context: Context) : OverlayViewModel(context) {

    /** The action being configured by the user. Defined using [setConfigAction]. */
    private val configuredAction = MutableStateFlow<Action?>(null)
    /** */
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        EVENT_CONFIG_PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    /** Tells if the configured action is valid and can be saved. */
    val isValidAction: Flow<Boolean> = configuredAction.map { action ->
        when (action) {
            null -> false
            is Action.Click -> action.x != null && action.y != null
            is Action.Swipe -> action.fromX != null && action.fromY != null && action.toX != null && action.toY != null
            is Action.Pause -> true
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
            }
        }
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

    /** Base class for observing/editing the values for an action. */
    abstract inner class ActionValues

    /** Allow to observe/edit the value a click action. */
    inner class ClickActionValues : ActionValues() {

        /** The name of the click. */
        val name: Flow<String?> = configuredAction.mapNotNull { click ->
            if (click is Action.Click) {
                click.name
            } else {
                null
            }
        }
        /** The duration between the press and release of the click in milliseconds. */
        val pressDuration: Flow<Long?> = configuredAction.map { click ->
            if (click is Action.Click && click.pressDuration != null) {
                click.pressDuration
            } else {
                sharedPreferences.getClickPressDurationConfig(context)
            }
        }
        /** The position of the click. */
        val position: Flow<Point?> = configuredAction.map { click ->
            if (click is Action.Click && click.x != null && click.y != null) {
                Point(click.x!!, click.y!!)
            } else {
                null
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
         * Get the click with all user changes.
         * Values provided by edit text are given here.
         *
         * @param clickName the name of the action.
         * @param duration the duration of the click press.
         *
         * @return the click containing all user changes.
         */
        fun getConfiguredClick(clickName: String, duration: Long): Action.Click =
            (configuredAction.value as Action.Click).apply {
                name = clickName
                pressDuration = duration
                sharedPreferences.edit().putClickPressDurationConfig(duration).apply()
            }
    }

    /** Allow to observe/edit the value a swipe action. */
    inner class SwipeActionValues : ActionValues() {

        /** The name of the click. */
        val name: Flow<String?> = configuredAction.mapNotNull { swipe ->
            if (swipe is Action.Swipe) {
                swipe.name
            } else {
                null
            }
        }
        /** The duration between the start and end of the swipe in milliseconds. */
        val swipeDuration: Flow<Long?> = configuredAction.map { swipe ->
            if (swipe is Action.Swipe && swipe.swipeDuration != null) {
                swipe.swipeDuration
            } else {
                sharedPreferences.getSwipeDurationConfig(context)
            }
        }
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
         * Get the swipe with all user changes.
         * Values provided by edit text are given here.
         *
         * @param swipeName the name of the action.
         * @param duration the duration of the swipe.
         *
         * @return the swipe containing all user changes.
         */
        fun getConfiguredSwipe(swipeName: String, duration: Long): Action.Swipe =
            (configuredAction.value as Action.Swipe).apply {
                name = swipeName
                swipeDuration = duration
                sharedPreferences.edit().putSwipeDurationConfig(duration).apply()
            }
    }

    /** Allow to observe/edit the value a pause action. */
    inner class PauseActionValues : ActionValues() {

        /** The name of the pause. */
        val name: Flow<String?> = configuredAction.mapNotNull { pause ->
            if (pause is Action.Pause) {
                pause.name
            } else {
                null
            }
        }
        /** The duration of the pause in milliseconds. */
        val pauseDuration: Flow<Long?> = configuredAction.map { pause ->
            if (pause is Action.Pause && pause.pauseDuration != null) {
                pause.pauseDuration
            } else {
                sharedPreferences.getPauseDurationConfig(context)
            }
        }

        /**
         * Get the pause with all user changes.
         * Values provided by edit text are given here.
         *
         * @param pauseName the name of the action.
         * @param duration the duration of the pause.
         *
         * @return the pause containing all user changes.
         */
        fun getConfiguredPause(pauseName: String, duration: Long): Action.Pause =
            (configuredAction.value as Action.Pause).apply {
                name = pauseName
                pauseDuration = duration
                sharedPreferences.edit().putPauseDurationConfig(duration).apply()
            }
    }
}
