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
package com.buzbuz.smartautoclicker.overlays.config.action.swipe

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Point

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.base.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.overlays.base.utils.isValidDuration
import com.buzbuz.smartautoclicker.overlays.base.utils.putSwipeDurationConfig

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SwipeViewModel(application: Application) : AndroidViewModel(application) {

    /** The action being configured by the user. Defined using [setConfiguredSwipe]. */
    private val configuredSwipe = MutableStateFlow<Action.Swipe?>(null)
    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = application.getEventConfigPreferences()

    /** The name of the swipe. */
    val name: Flow<String?> = configuredSwipe
        .filterNotNull()
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredSwipe.map { it?.name?.isEmpty() ?: true }

    /** The duration between the start and end of the swipe in milliseconds. */
    val swipeDuration: Flow<String?> = configuredSwipe
        .filterNotNull()
        .map { it.swipeDuration?.toString() }
        .take(1)
    /** Tells if the swipe duration value is valid or not. */
    val swipeDurationError: Flow<Boolean> = configuredSwipe.map { (it?.swipeDuration ?: -1) <= 0 }

    /** The start and end positions of the swipe. */
    val positions: Flow<Pair<Point, Point>?> = configuredSwipe
        .filterNotNull()
        .map { swipe ->
            if (swipe.fromX != null && swipe.fromY != null && swipe.toX != null && swipe.toY != null) {
                Point(swipe.fromX!!, swipe.fromY!!) to Point(swipe.toX!!, swipe.toY!!)
            } else {
                null
            }
        }

    /** Tells if the configured swipe is valid and can be saved. */
    val isValidAction: Flow<Boolean> = configuredSwipe
        .map { swipe ->
            swipe != null && !swipe.name.isNullOrEmpty()
                    && swipe.fromX != null && swipe.fromY != null && swipe.toX != null && swipe.toY != null
                    && swipe.swipeDuration.isValidDuration()
        }

    /**
     * Set the configured swipe.
     * This will update all values represented by this view model.
     *
     * @param swipe the swipe to configure.
     */
    fun setConfiguredSwipe(swipe: Action.Swipe) {
        configuredSwipe.value = swipe.deepCopy()
    }

    /** @return the swipe containing all user changes. */
    fun getConfiguredSwipe(): Action.Swipe =
        configuredSwipe.value ?: throw IllegalStateException("Can't get the configured swipe, none were defined.")

    /**
     * Set the name of the swipe.
     * @param name the new name.
     */
    fun setName(name: String) {
        configuredSwipe.value?.let { swipe ->
            configuredSwipe.value = swipe.copy(name = "" + name)
        }
    }

    /**
     * Set the start and end positions of the swipe.
     * @param from the new start position.
     * @param to the new end position.
     */
    fun setPositions(from: Point, to: Point) {
        configuredSwipe.value?.let { swipe ->
            configuredSwipe.value = swipe.copy(fromX = from.x, fromY = from.y, toX = to.x, toY = to.y)
        }
    }

    /**
     * Set the duration of the swipe.
     * @param durationMs the new duration in milliseconds.
     */
    fun setSwipeDuration(durationMs: Long?) {
        configuredSwipe.value?.let { swipe ->
            configuredSwipe.value = swipe.copy(swipeDuration = durationMs)
        }
    }

    fun saveLastConfig() {
        configuredSwipe.value?.let { swipe ->
            sharedPreferences.edit().putSwipeDurationConfig(swipe.swipeDuration ?: 0).apply()
        }
    }
}