/*
 * Copyright (C) 2022 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.config.condition

import android.app.Application
import android.graphics.Bitmap

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.*
import com.buzbuz.smartautoclicker.overlays.base.bindings.DropdownItem

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

    private val shouldBeDetectedItem = DropdownItem(
        title = R.string.dropdown_item_title_condition_visibility_present,
        helperText = R.string.dropdown_helper_text_condition_visibility_present,
        icon = R.drawable.ic_confirm,
    )
    private val shouldNotBeDetectedItem = DropdownItem(
        title= R.string.dropdown_item_title_condition_visibility_absent,
        helperText = R.string.dropdown_helper_text_condition_visibility_absent,
        icon = R.drawable.ic_cancel,
    )
    /**  Should be detected choices for the dropdown field. */
    val shouldBeDetectedItems = listOf(shouldBeDetectedItem, shouldNotBeDetectedItem)
    /** Tells if the condition should be present or not on the screen. */
    val shouldBeDetected: Flow<DropdownItem> = configuredCondition
        .mapNotNull { condition ->
            when (condition?.shouldBeDetected) {
                true -> shouldBeDetectedItem
                false -> shouldNotBeDetectedItem
                null -> null
            }
        }
        .filterNotNull()

    private val detectionTypeExact = DropdownItem(
        title = R.string.dropdown_item_title_detection_type_exact,
        helperText = R.string.dropdown_helper_text_detection_type_exact,
        icon = R.drawable.ic_detect_exact,
    )
    private val detectionTypeScreen = DropdownItem(
        title= R.string.dropdown_item_title_detection_type_screen,
        helperText = R.string.dropdown_helper_text_detection_type_screen,
        icon = R.drawable.ic_detect_whole_screen,
    )
    /** Detection types choices for the dropdown field. */
    val detectionTypeItems = listOf(detectionTypeExact, detectionTypeScreen)
    /** The type of detection currently selected by the user. */
    val detectionType: Flow<DropdownItem> = configuredCondition
        .map { condition ->
            when (condition?.detectionType) {
                EXACT -> detectionTypeExact
                WHOLE_SCREEN -> detectionTypeScreen
                else -> null
            }
        }
        .filterNotNull()

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

    /** Set the shouldBeDetected value of the condition. */
    fun setShouldBeDetected(newShouldBeDetected: DropdownItem) {
        configuredCondition.value?.let { condition ->
            val shouldBeDetected = when (newShouldBeDetected) {
                shouldBeDetectedItem -> true
                shouldNotBeDetectedItem -> false
                else -> return
            }

            configuredCondition.value = condition.copy(shouldBeDetected = shouldBeDetected)
        } ?: throw IllegalStateException("Can't toggle condition should be detected, condition is null!")
    }

    /** Set the detection type. */
    fun setDetectionType(newType: DropdownItem) {
        configuredCondition.value?.let { condition ->
            val type = when (newType) {
                detectionTypeExact -> EXACT
                detectionTypeScreen -> WHOLE_SCREEN
                else -> return
            }

            configuredCondition.value = condition.copy(detectionType = type)
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