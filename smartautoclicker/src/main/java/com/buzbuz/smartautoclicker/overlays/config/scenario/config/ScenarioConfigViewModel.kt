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
package com.buzbuz.smartautoclicker.overlays.config.scenario.config

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.detection.DETECTION_QUALITY_MAX
import com.buzbuz.smartautoclicker.detection.DETECTION_QUALITY_MIN
import com.buzbuz.smartautoclicker.domain.*

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

/**
 * View model for the [ScenarioSettingsDialog].
 *
 * @param context the Android context.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioConfigViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application)

    /** The scenario configured. */
    private val configuredScenario = MutableStateFlow<Scenario?>(null)
    /** The list of end condition configured. */
    val configuredEndConditions = MutableStateFlow<List<EndCondition>>(emptyList())

    /** The event name value currently edited by the user. */
    val scenarioName: Flow<String> = configuredScenario.map { it?.name }
        .filterNotNull()
        .take(1)
    /** The quality of the detection. */
    val detectionQuality = configuredScenario.map { it?.detectionQuality }
    /** The operator applied to the end conditions. */
    val endConditionOperator = configuredScenario.map { it?.endConditionOperator }

    /** List of events for this scenario. */
    private val eventList = configuredScenario
        .filterNotNull()
        .flatMapLatest { repository.getEventList(it.id) }
    /** Events available for a new end condition. */
    private val eventsAvailable = configuredEndConditions
        .combine(eventList) { endConditions, events ->
            if (endConditions.isEmpty()) events.isNotEmpty()
            else events.any { event -> endConditions.find { it.eventId == event.id } == null }
        }
    /** The end conditions for the configured scenario. */
    val endConditions = configuredEndConditions.combine(eventsAvailable) { endConditions, eventsAvailable ->
        buildList {
            endConditions.forEach { add(EndConditionListItem.EndConditionItem(it)) }
            if (eventsAvailable) add(EndConditionListItem.AddEndConditionItem)
        }
    }

    /** Tells if the current config is valid or not. */
    val isValidConfig: Flow<Boolean> = configuredScenario.map { scenario ->
        scenario?.name?.isNotEmpty() == true
    }

    /**
     * Set the scenario to be configured.
     * @param id the unique identifier of the scenario.
     */
    fun setScenario(id: Long) {
        viewModelScope.launch {
            val scenarioWithEndConditions = repository.getScenarioWithEndConditions(id)
            configuredScenario.value = scenarioWithEndConditions.first
            configuredEndConditions.value = scenarioWithEndConditions.second
        }
    }

    /** Set a new name for the scenario. */
    fun setScenarioName(name: String) {
        configuredScenario.value?.let { scenario ->
            configuredScenario.value = scenario.copy(name = name)
        }
    }

    /** Remove one to the detection quality */
    fun decreaseDetectionQuality() {
        configuredScenario.value?.let { scenario ->
            configuredScenario.value = scenario.copy(
                detectionQuality = max(scenario.detectionQuality - 1, DETECTION_QUALITY_MIN.toInt())
            )
        }
    }

    /** Add one to the detection quality */
    fun increaseDetectionQuality() {
        configuredScenario.value?.let { scenario ->
            configuredScenario.value = scenario.copy(
                detectionQuality = min(scenario.detectionQuality + 1, DETECTION_QUALITY_MAX.toInt())
            )
        }
    }

    /**
     * Set the detection quality for the scenario.
     * @param quality the value from the seekbar.
     */
    fun setDetectionQuality(quality: Int) {
        configuredScenario.value?.let { scenario ->
            configuredScenario.value = scenario.copy(detectionQuality = quality)
        }
    }

    /** Toggle the end condition operator between AND and OR. */
    fun setConditionOperator(@ConditionOperator operator: Int) {
        configuredScenario.value?.let { scenario ->
            configuredScenario.value = scenario.copy(endConditionOperator = operator)
        }
    }

    /** @return a new empty end condition. */
    fun createNewEndCondition() =
        EndCondition(scenarioId = configuredScenario.value?.id ?: 0)

    /**
     * Add a new end condition to the scenario.
     * @param endCondition the end condition to be added.
     */
    fun addEndCondition(endCondition: EndCondition) {
        if (endCondition.id == 0L) {
            configuredEndConditions.value = configuredEndConditions.value
                .toMutableList()
                .apply {
                    add(endCondition)
                }
        }
    }

    /**
     * Update an end condition from the scenario.
     * @param endCondition the end condition to be updated.
     */
    fun updateEndCondition(endCondition: EndCondition, index: Int) {
        val newConditions = ArrayList(configuredEndConditions.value)
        newConditions[index] = endCondition
        configuredEndConditions.value = newConditions
    }

    /**
     * Delete a end condition from the scenario.
     * @param endCondition the end condition to be removed.
     */
    fun deleteEndCondition(endCondition: EndCondition) {
        val newConditions = ArrayList(configuredEndConditions.value)
        if (newConditions.remove(endCondition)) {
            configuredEndConditions.value = newConditions
        }
    }

    /** Save the scenario settings modifications in database. */
    fun saveModifications() {
        viewModelScope.launch(Dispatchers.IO) {
            configuredScenario.value?.let { scenario ->
                repository.updateScenario(scenario)
                repository.updateEndConditions(scenario.id, configuredEndConditions.value)
            }
        }
    }
}

/** Items displayed in the end condition list. */
sealed class EndConditionListItem {
    /** The add end condition item. */
    object AddEndConditionItem : EndConditionListItem()
    /** Item representing a end condition. */
    data class EndConditionItem(val endCondition: EndCondition) : EndConditionListItem()
}

/** The minimum value for the seek bar. */
const val SLIDER_QUALITY_MIN = DETECTION_QUALITY_MIN.toFloat()
/** The maximum value for the seek bar. */
const val SLIDER_QUALITY_MAX = DETECTION_QUALITY_MAX.toFloat()