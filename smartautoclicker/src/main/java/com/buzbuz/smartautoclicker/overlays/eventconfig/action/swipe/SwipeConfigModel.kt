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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.swipe

import android.content.SharedPreferences
import android.graphics.Point

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ActionModel
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.isValidDuration
import com.buzbuz.smartautoclicker.overlays.utils.putSwipeDurationConfig

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * Allow to observe/edit the values a swipe action.
 *
 * @param viewModelScope the scope for the view model holding this model.
 * @param configuredAction the flow on the edited action.
 */
class SwipeConfigModel(
    private val viewModelScope: CoroutineScope,
    private val configuredAction: MutableStateFlow<Action?>,
) : ActionModel() {

    override val isValidAction: Flow<Boolean> = configuredAction
        .map { action ->
            action is Action.Swipe
                    && !action.name.isNullOrEmpty()
                    && action.fromX != null && action.fromY != null && action.toX != null && action.toY != null
                    && action.swipeDuration.isValidDuration()
        }

    override fun saveLastConfig(eventConfigPrefs: SharedPreferences) {
        configuredAction.value?.let { action ->
            if (action is Action.Swipe) {
                eventConfigPrefs.edit().putSwipeDurationConfig(action.swipeDuration ?: 0).apply()
            }
        }
    }

    /** The duration between the start and end of the swipe in milliseconds. */
    val swipeDuration: Flow<Long?> = configuredAction
        .filterIsInstance<Action.Swipe>()
        .map { it.swipeDuration }
        .take(1)
    /** The start and end positions of the swipe. */
    val positions: Flow<Pair<Point?, Point?>> = configuredAction
        .filterIsInstance<Action.Swipe>()
        .map { swipe ->
            if (swipe.fromX != null && swipe.fromY != null
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
                configuredAction.value = swipe.copy(fromX = from.x, fromY = from.y, toX = to.x, toY = to.y)
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
                configuredAction.value = swipe.copy(swipeDuration = durationMs)
            }
        }
    }
}