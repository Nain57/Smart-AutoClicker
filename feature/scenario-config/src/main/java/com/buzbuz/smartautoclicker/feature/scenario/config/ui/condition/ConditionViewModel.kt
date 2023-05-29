/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition

import android.app.Application
import android.graphics.Bitmap

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.ui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.flow.*

class ConditionViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the database. */
    private val repository = Repository.getRepository(application)
    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedConditionState
        .mapNotNull { it.value }

    /** The type of detection currently selected by the user. */
    val name: Flow<String?> = configuredCondition.map { it.name }.take(1)
    /** Tells if the condition name is valid or not. */
    val nameError: Flow<Boolean> = configuredCondition.map { it.name.isEmpty() }

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
            when (condition.shouldBeDetected) {
                true -> shouldBeDetectedItem
                false -> shouldNotBeDetectedItem
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
            when (condition.detectionType) {
                EXACT -> detectionTypeExact
                WHOLE_SCREEN -> detectionTypeScreen
                else -> null
            }
        }
        .filterNotNull()

    /** The condition threshold value currently edited by the user. */
    val threshold: Flow<Int> = configuredCondition.mapNotNull { it.threshold }
    /** The bitmap for the configured condition. */
    val conditionBitmap: Flow<Bitmap?> = configuredCondition.map { condition ->
        if (condition.bitmap != null) return@map condition.bitmap

        condition.path?.let { path ->
            repository.getBitmap(path, condition.area.width(), condition.area.height())
        }
    }
    /** Tells if the configured condition is valid and can be saved. */
    val conditionCanBeSaved: Flow<Boolean> = editionRepository.editionState.editedConditionState.map { condition ->
        condition.canBeSaved
    }

    /**
     * Set the configured condition name.
     * @param name the new condition name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedCondition()?.let { condition ->
            editionRepository.updateEditedCondition(condition.copy(name = name))
        }
    }

    /** Set the shouldBeDetected value of the condition. */
    fun setShouldBeDetected(newShouldBeDetected: DropdownItem) {
        editionRepository.editionState.getEditedCondition()?.let { condition ->
            val shouldBeDetected = when (newShouldBeDetected) {
                shouldBeDetectedItem -> true
                shouldNotBeDetectedItem -> false
                else -> return
            }

            editionRepository.updateEditedCondition(condition.copy(shouldBeDetected = shouldBeDetected))
        }
    }

    /** Set the detection type. */
    fun setDetectionType(newType: DropdownItem) {
        editionRepository.editionState.getEditedCondition()?.let { condition ->
            val type = when (newType) {
                detectionTypeExact -> EXACT
                detectionTypeScreen -> WHOLE_SCREEN
                else -> return
            }

            editionRepository.updateEditedCondition(condition.copy(detectionType = type))
        }
    }

    /**
     * Set the threshold of the configured condition.
     * @param value the new threshold value.
     */
    fun setThreshold(value: Int) {
        editionRepository.editionState.getEditedCondition()?.let { condition ->
            editionRepository.updateEditedCondition(condition.copy(threshold = value))
        }
    }
}

/** The maximum threshold value selectable by the user. */
const val MAX_THRESHOLD = 20f