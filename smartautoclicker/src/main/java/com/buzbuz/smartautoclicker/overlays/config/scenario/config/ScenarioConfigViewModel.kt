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
package com.buzbuz.smartautoclicker.overlays.config.scenario.config

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.detection.DETECTION_QUALITY_MAX
import com.buzbuz.smartautoclicker.detection.DETECTION_QUALITY_MIN
import com.buzbuz.smartautoclicker.domain.*
import com.buzbuz.smartautoclicker.domain.edition.EditedEndCondition
import com.buzbuz.smartautoclicker.domain.edition.EditionRepository

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * View model for the [ScenarioConfigContent].
 *
 * @param application the Android application.
 */
class ScenarioConfigViewModel(application: Application) : AndroidViewModel(application) {

    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** Currently configured scenario. */
    private val configuredScenario = editionRepository.editedScenario
        .filterNotNull()

    /** The event name value currently edited by the user. */
    val scenarioName: Flow<String> =  configuredScenario
        .map { it.scenario.name }
        .filterNotNull()
        .take(1)
    /** Tells if the scenario name is valid or not. */
    val scenarioNameError: Flow<Boolean> = configuredScenario
        .map { it.scenario.name.isEmpty() }

    private val enabledRandomization = DropdownItem(
        title = R.string.dropdown_item_title_anti_detection_enabled,
        helperText = R.string.dropdown_helper_text_anti_detection_enabled,
    )
    private val disableRandomization = DropdownItem(
        title = R.string.dropdown_item_title_anti_detection_disabled,
        helperText = R.string.dropdown_helper_text_anti_detection_disabled,
    )
    val randomizationItems = listOf(enabledRandomization, disableRandomization)

    /** The randomization value for the scenario. */
    val randomization: Flow<DropdownItem> = configuredScenario
        .map {
            when (it.scenario.randomize) {
                true -> enabledRandomization
                false -> disableRandomization
            }
        }
        .filterNotNull()

    /** The quality of the detection. */
    val detectionQuality: Flow<Int?> = configuredScenario
        .map { it.scenario.detectionQuality }

    private val conditionAndItem = DropdownItem(
        title = R.string.dropdown_item_title_condition_and,
        helperText = R.string.dropdown_helper_text_end_condition_and,
    )
    private val conditionOrItem = DropdownItem(
        title= R.string.dropdown_item_title_condition_or,
        helperText = R.string.dropdown_helper_text_end_condition_or,
    )
    val endConditionOperatorsItems = listOf(conditionAndItem, conditionOrItem)

    /** The operator applied to the end conditions. */
    val endConditionOperator: Flow<DropdownItem> = configuredScenario
        .map {
            when (it.scenario.endConditionOperator) {
                AND -> conditionAndItem
                OR -> conditionOrItem
                else -> null
            }
        }
        .filterNotNull()


    /** Events available for a new end condition. */
    private val eventsAvailable: Flow<Boolean> = configuredScenario.map { confScenario ->
        if (confScenario.endConditions.isEmpty()) confScenario.events.isNotEmpty()
        else confScenario.events.any { confEvent ->
            confScenario.endConditions.find { it.endCondition.eventId == confEvent.event.id } == null
        }
    }

    /** The end conditions for the configured scenario. */
    val endConditions: Flow<List<EndConditionListItem>> =
        configuredScenario.combine(eventsAvailable) { confScenario, eventsAvailable ->
            buildList {
                confScenario.endConditions.forEach { add(EndConditionListItem.EndConditionItem(it)) }
                if (eventsAvailable) add(EndConditionListItem.AddEndConditionItem)
            }
        }

    /** Set a new name for the scenario. */
    fun setScenarioName(name: String) {
        editionRepository.editedScenario.value?.let { conf ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(conf.scenario.copy(name = name))
            }
        }
    }

    /** Toggle the randomization value. */
    fun setRandomization(randomizationItem: DropdownItem) {
        editionRepository.editedScenario.value?.let { conf ->
            val value = when (randomizationItem) {
                enabledRandomization -> true
                disableRandomization -> false
                else -> return
            }

            viewModelScope.launch {
                editionRepository.updateEditedScenario(conf.scenario.copy(randomize = value))
            }
        }
    }

    /** Remove one to the detection quality */
    fun decreaseDetectionQuality() {
        editionRepository.editedScenario.value?.let { conf ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(
                    conf.scenario.copy(
                        detectionQuality = max(conf.scenario.detectionQuality - 1, DETECTION_QUALITY_MIN.toInt())
                    )
                )
            }
        }
    }

    /** Add one to the detection quality */
    fun increaseDetectionQuality() {
        editionRepository.editedScenario.value?.let { conf ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(
                    conf.scenario.copy(
                        detectionQuality = min(conf.scenario.detectionQuality + 1, DETECTION_QUALITY_MAX.toInt())
                    )
                )
            }
        }
    }

    /**
     * Set the detection quality for the scenario.
     * @param quality the value from the seekbar.
     */
    fun setDetectionQuality(quality: Int) {
        editionRepository.editedScenario.value?.let { conf ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(conf.scenario.copy(detectionQuality = quality))
            }
        }
    }

    /** Toggle the end condition operator between AND and OR. */
    fun setConditionOperator(operatorItem: DropdownItem) {
        editionRepository.editedScenario.value?.let { conf ->
            val operator = when (operatorItem) {
                conditionAndItem -> AND
                conditionOrItem -> OR
                else -> return
            }

            viewModelScope.launch {
                editionRepository.updateEditedScenario(conf.scenario.copy(endConditionOperator = operator))
            }
        }
    }

    /** @return a new empty end condition. */
    fun createNewEndCondition(): EditedEndCondition =
        editionRepository.createNewEndCondition()

    /**
     * Add a new end condition to the scenario.
     * @param confEndCondition the end condition to be added.
     */
    fun addEndCondition(confEndCondition: EditedEndCondition) =
        editionRepository.addEndCondition(confEndCondition)

    /**
     * Update an end condition from the scenario.
     * @param confEndCondition the end condition to be updated.
     */
    fun updateEndCondition(confEndCondition: EditedEndCondition) =
        editionRepository.updateEndCondition(confEndCondition)

    /**
     * Delete a end condition from the scenario.
     * @param confEndCondition the end condition to be removed.
     */
    fun deleteEndCondition(confEndCondition: EditedEndCondition) =
        editionRepository.deleteEndCondition(confEndCondition)
}

/** Items displayed in the end condition list. */
sealed class EndConditionListItem {
    /** The add end condition item. */
    object AddEndConditionItem : EndConditionListItem()
    /** Item representing a end condition. */
    data class EndConditionItem(val endCondition: EditedEndCondition) : EndConditionListItem()
}

/** The minimum value for the seek bar. */
const val SLIDER_QUALITY_MIN = DETECTION_QUALITY_MIN.toFloat()
/** The maximum value for the seek bar. */
const val SLIDER_QUALITY_MAX = DETECTION_QUALITY_MAX.toFloat()