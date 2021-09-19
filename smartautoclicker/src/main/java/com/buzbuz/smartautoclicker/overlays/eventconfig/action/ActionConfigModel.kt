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
import android.graphics.Point

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.database.domain.Action

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
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

    /**
     * Job for updating [configuredAction] values that depends on an EditText.
     * As the user can stop the EditText edition with a lots of different ways, it is difficult to tell exactly when the
     * user is done editing. As a solution, we listen to each text edition and call the model for an update. But those
     * calls can be numerous and this leads to a slow UI feeling when editing.
     * So we delay those calls using this [Job] by [EDIT_TEXT_UPDATE_DELAY] to only update once after the user have
     * stopped editing for a moment.
     * This Job is null when the user isn't editing.
     */
    private var editJob: Job? = null

    /** Tells if the configured action is valid and can be saved. */
    val isValidAction: Flow<Boolean> = configuredAction.map { action ->
        when (action) {
            null -> false
            is Action.Click -> !action.name.isNullOrEmpty() && action.pressDuration != null && action.x != null
                    && action.y != null
            is Action.Swipe -> !action.name.isNullOrEmpty() && action.swipeDuration != null && action.fromX != null
                    && action.fromY != null && action.toX != null && action.toY != null
            is Action.Pause -> !action.name.isNullOrEmpty() && action.pauseDuration != null
        }
    }

    /** The values for the [configuredAction]. Type will change according to the action type. */
    val actionValues = configuredAction.mapNotNull { action ->
        @Suppress("UNCHECKED_CAST") // Nullity is handled first
        when (action) {
            null -> null
            is Action.Click -> ClickActionValues()
            is Action.Swipe -> SwipeActionValues()
            is Action.Pause -> PauseActionValues()
        }
    }

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

    /** @return the condition containing all user changes. */
    fun getConfiguredAction(): Action =
        configuredAction.value ?: throw IllegalStateException("Can't get the configured action, none were defined.")

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
            if (click is Action.Click) {
                click.pressDuration
            } else {
                null
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
         * Set the name of the configured click action.
         * @param name the new name.
         */
        fun setName(name: String) {
            (configuredAction.value as Action.Click).let { click ->
                viewModelScope.launch {
                    delay(EDIT_TEXT_UPDATE_DELAY)
                    configuredAction.emit(click.copy(name = name))
                }
            }
        }

        /**
         * Set the duration between the press and release of the click.
         * @param durationMs the new duration in milliseconds.
         */
        fun setPressDuration(durationMs: Long) {
            (configuredAction.value as Action.Click).let { click ->
                editJob?.cancel()
                editJob = viewModelScope.launch {
                    delay(EDIT_TEXT_UPDATE_DELAY)
                    configuredAction.emit(click.copy(pressDuration = durationMs))
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
            if (swipe is Action.Swipe) {
                swipe.swipeDuration
            } else {
                null
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
         * Set the name of the configured swipe action.
         * @param name the new name.
         */
        fun setName(name: String) {
            (configuredAction.value as Action.Swipe).let { swipe ->
                editJob?.cancel()
                editJob = viewModelScope.launch {
                    delay(EDIT_TEXT_UPDATE_DELAY)
                    configuredAction.emit(swipe.copy(name = name))
                }
            }
        }

        /**
         * Set the duration between the start and end of the swipe.
         * @param durationMs the new duration in milliseconds.
         */
        fun setSwipeDuration(durationMs: Long) {
            (configuredAction.value as Action.Swipe).let { swipe ->
                editJob?.cancel()
                editJob = viewModelScope.launch {
                    delay(EDIT_TEXT_UPDATE_DELAY)
                    configuredAction.emit(swipe.copy(swipeDuration = durationMs))
                }
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
            if (pause is Action.Pause) {
                pause.pauseDuration
            } else {
                null
            }
        }

        /**
         * Set the name of the configured pause action.
         * @param name the new name.
         */
        fun setName(name: String) {
            (configuredAction.value as Action.Pause).let { pause ->
                editJob?.cancel()
                editJob = viewModelScope.launch {
                    delay(EDIT_TEXT_UPDATE_DELAY)
                    configuredAction.emit(pause.copy(name = name))
                }
            }
        }

        /**
         * Set the pause duration.
         * @param durationMs the new duration in milliseconds.
         */
        fun setPauseDuration(durationMs: Long) {
            (configuredAction.value as Action.Pause).let { pause ->
                editJob?.cancel()
                editJob = viewModelScope.launch {
                    delay(EDIT_TEXT_UPDATE_DELAY)
                    configuredAction.emit(pause.copy(pauseDuration = durationMs))
                }
            }
        }
    }
}

/** Delay without update before updating the action. */
private const val EDIT_TEXT_UPDATE_DELAY = 750L