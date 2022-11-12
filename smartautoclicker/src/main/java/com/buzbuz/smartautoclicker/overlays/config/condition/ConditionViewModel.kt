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
package com.buzbuz.smartautoclicker.overlays.config.condition

import android.app.Application
import android.graphics.Bitmap

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.domain.*

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ConditionViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the database. */
    private val repository = Repository.getRepository(application)

    /** The condition being configured by the user. Defined using [setConfigCondition]. */
    private val configuredCondition = MutableStateFlow<Condition?>(null)
    /** The type of detection currently selected by the user. */
    val name: Flow<String?> = configuredCondition.map { it?.name }.take(1)
    /** Tells if the condition name is valid or not. */
    val nameError: Flow<Boolean> = configuredCondition.map { it?.name?.isEmpty() ?: true }
    /** Tells if the condition should be present or not on the screen. */
    val shouldBeDetected: Flow<Boolean> = configuredCondition.mapNotNull { it?.shouldBeDetected }
    /** The type of detection currently selected by the user. */
    val detectionType: Flow<Int> = configuredCondition.mapNotNull { it?.detectionType }
    /** The condition threshold value currently edited by the user. */
    val threshold: Flow<Int> = configuredCondition.mapNotNull { it?.threshold }
    /** The bitmap for the configured condition. */
    val conditionBitmap: Flow<Bitmap?> = configuredCondition.map { condition ->
        if (condition == null) return@map null
        if (condition.bitmap != null) return@map condition.bitmap

        condition.path?.let { path ->
            repository.getBitmap(path, condition.area.width(), condition.area.height())
        }
    }
    /** Tells if the configured condition is valid and can be saved. */
    val isValidCondition: Flow<Boolean> = configuredCondition.map { condition ->
        condition != null && condition.name.isNotEmpty()
    }

    /**
     * Set the configured condition.
     * This will update all values represented by this view model.
     *
     * @param condition the condition to configure.
     */
    fun setConfigCondition(condition: Condition) {
        viewModelScope.launch {
            configuredCondition.emit(condition.deepCopy())
        }
    }

    /** @return the condition containing all user changes. */
    fun getConfiguredCondition(): Condition =
        configuredCondition.value ?: throw IllegalStateException("Can't get the configured condition, none were defined.")

    /**
     * Set the configured condition name.
     * @param name the new condition name.
     */
    fun setName(name: String) {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(name = name)
        } ?: throw IllegalStateException("Can't set condition name, condition is null!")
    }

    /** Toggle between true and false for the shouldBeDetected value of the condition. */
    fun setShouldBeDetected(newShouldBeDetected: Boolean) {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(shouldBeDetected = newShouldBeDetected)
        } ?: throw IllegalStateException("Can't toggle condition should be detected, condition is null!")
    }

    /** Toggle between exact and whole screen for the detection type. */
    fun setDetectionType(@DetectionType newType: Int) {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(detectionType = newType)
        } ?: throw IllegalStateException("Can't toggle condition should be detected, condition is null!")
    }

    /**
     * Set the threshold of the configured condition.
     * @param value the new threshold value.
     */
    fun setThreshold(value: Int) {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(threshold = value)
        }
    }
}

/** The maximum threshold value selectable by the user. */
const val MAX_THRESHOLD = 20f