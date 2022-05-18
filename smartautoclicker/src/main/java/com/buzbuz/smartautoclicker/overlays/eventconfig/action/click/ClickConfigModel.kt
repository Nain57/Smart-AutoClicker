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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.click

import android.content.SharedPreferences
import android.graphics.Point

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ActionModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.isValidDuration
import com.buzbuz.smartautoclicker.overlays.utils.putClickPressDurationConfig

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * Allow to observe/edit the values a click action.
 *
 * @param viewModelScope the scope for the view model holding this model.
 * @param configuredAction the flow on the edited action.
 */
class ClickConfigModel(
    private val viewModelScope: CoroutineScope,
    private val configuredAction: MutableStateFlow<Action?>,
) : ActionModel() {

    override val isValidAction: Flow<Boolean> = configuredAction
        .map { action ->
            action is Action.Click
                    && !action.name.isNullOrEmpty()
                    && ((action.x != null && action.y != null) || action.clickOnCondition)
                    && action.pressDuration.isValidDuration()
        }

    override fun saveLastConfig(eventConfigPrefs: SharedPreferences) {
        configuredAction.value?.let { action ->
            if (action is Action.Click) {
                eventConfigPrefs.edit().putClickPressDurationConfig(action.pressDuration ?: 0).apply()
            }
        }
    }

    /** The duration between the press and release of the click in milliseconds. */
    val pressDuration: Flow<Long?> = configuredAction
        .filterIsInstance<Action.Click>()
        .map { it.pressDuration }
        .take(1)
    /** The position of the click. */
    val position: Flow<Point?> = configuredAction
        .filterIsInstance<Action.Click>()
        .map { click ->
            if (click.x != null && click.y != null) {
                Point(click.x!!, click.y!!)
            } else {
                null
            }
        }
    /** If the click should be made on the detected condition. */
    val clickOnCondition: Flow<Boolean> = configuredAction
        .filterIsInstance<Action.Click>()
        .map { click ->
            click.clickOnCondition
        }

    /**
     * Set if this click should be made on the detected condition.
     * @param enabled true to click on the detected condition, false to let the user pick its own location.
     */
    fun setClickOnCondition(enabled: Boolean) {
        (configuredAction.value as Action.Click).let { click ->
            viewModelScope.launch {
                configuredAction.value = click.copy(clickOnCondition = enabled)
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
                configuredAction.value = click.copy(x = position.x, y = position.y)
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
                configuredAction.value = click.copy(pressDuration = durationMs)
            }
        }
    }
}