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
package com.buzbuz.smartautoclicker.overlays.eventconfig.condition

import android.content.Context
import android.graphics.Bitmap

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.domain.EXACT
import com.buzbuz.smartautoclicker.domain.WHOLE_SCREEN

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View model for the [ConditionConfigDialog].
 * @param context the Android context.
 */
class ConditionConfigModel(context: Context) : OverlayViewModel(context) {

    /** Repository providing access to the database. */
    private val repository = Repository.getRepository(context)

    /** The condition being configured by the user. Defined using [setConfigCondition]. */
    private val configuredCondition = MutableStateFlow<Condition?>(null)
    /** The type of detection currently selected by the user. */
    val name: Flow<String?> = configuredCondition.map { it?.name }.take(1)
    /** Tells if the condition should be present or not on the screen. */
    val shouldBeDetected: Flow<Boolean> = configuredCondition.mapNotNull { it?.shouldBeDetected }
    /** The type of detection currently selected by the user. */
    val detectionType: Flow<Int> = configuredCondition.mapNotNull { it?.detectionType }
    /** The condition threshold value currently edited by the user. */
    val threshold: Flow<Int> = configuredCondition.mapNotNull { it?.threshold }
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
    fun toggleShouldBeDetected() {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(shouldBeDetected = !condition.shouldBeDetected)
        } ?: throw IllegalStateException("Can't toggle condition should be detected, condition is null!")
    }

    /** Toggle between exact and whole screen for the detection type. */
    fun toggleDetectionType() {
        configuredCondition.value?.let { condition ->
            configuredCondition.value = condition.copy(
                detectionType = if (condition.detectionType == EXACT) WHOLE_SCREEN else EXACT
            )
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

/** The maximum threshold value selectable by the user. */
const val MAX_THRESHOLD = 20