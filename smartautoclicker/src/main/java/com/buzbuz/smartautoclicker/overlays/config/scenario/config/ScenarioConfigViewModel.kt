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
import com.buzbuz.smartautoclicker.R

import com.buzbuz.smartautoclicker.detection.DETECTION_QUALITY_MAX
import com.buzbuz.smartautoclicker.detection.DETECTION_QUALITY_MIN
import com.buzbuz.smartautoclicker.domain.*
import com.buzbuz.smartautoclicker.overlays.base.bindings.DropdownItem
import com.buzbuz.smartautoclicker.overlays.config.scenario.ConfiguredScenario

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.max
import kotlin.math.min

/**
 * View model for the [ScenarioConfigContent].
 *
 * @param application the Android application.
 */
class ScenarioConfigViewModel(application: Application) : AndroidViewModel(application) {

    /** The scenario configured. */
    private lateinit var configuredScenario: MutableStateFlow<ConfiguredScenario?>

    /** The event name value currently edited by the user. */
    val scenarioName: Flow<String> by lazy {
        configuredScenario.map { it?.scenario?.name }
            .filterNotNull()
            .take(1)
    }
    /** Tells if the scenario name is valid or not. */
    val scenarioNameError: Flow<Boolean> by lazy {
        configuredScenario.map { it?.scenario?.name?.isEmpty() ?: true }
    }
    /** The quality of the detection. */
    val detectionQuality: Flow<Int?> by lazy {
        configuredScenario.map { it?.scenario?.detectionQuality }
    }

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
    val endConditionOperator: Flow<DropdownItem> by lazy {
        configuredScenario
            .map {
                when (it?.scenario?.endConditionOperator) {
                    AND -> conditionAndItem
                    OR -> conditionOrItem
                    else -> null
                }
            }
            .filterNotNull()
    }

    /** Events available for a new end condition. */
    private val eventsAvailable: Flow<Boolean> by lazy {
        configuredScenario.map { confScenario ->
                if (confScenario == null) return@map false
                if (confScenario.endConditions.isEmpty()) confScenario.events.isNotEmpty()
                else confScenario.events.any { confEvent ->
                    confScenario.endConditions.find { it.eventId == confEvent.event.id } == null
                }
        }
    }
    /** The end conditions for the configured scenario. */
    val endConditions: Flow<List<EndConditionListItem>> by lazy {
        configuredScenario.combine(eventsAvailable) { confScenario, eventsAvailable ->
            if (confScenario == null) return@combine emptyList()
            buildList {
                confScenario.endConditions.forEach { add(EndConditionListItem.EndConditionItem(it)) }
                if (eventsAvailable) add(EndConditionListItem.AddEndConditionItem)
            }
        }
    }

    /**
     * Set the scenario to be configured.
     * @param scenario the scenario.
     */
    fun setScenario(scenario: MutableStateFlow<ConfiguredScenario?>) {
        configuredScenario = scenario
    }

    /** Get all end conditions currently configured in this scenario. */
    fun getConfiguredEndConditionsList(): List<EndCondition> = configuredScenario.value?.endConditions ?: emptyList()

    /** Set a new name for the scenario. */
    fun setScenarioName(name: String) {
        configuredScenario.value?.let { conf ->
            configuredScenario.value = conf.scenario.copy(name = name).toScenarioConfig(conf)
        }
    }

    /** Remove one to the detection quality */
    fun decreaseDetectionQuality() {
        configuredScenario.value?.let { conf ->
            configuredScenario.value = conf.scenario.copy(
                detectionQuality = max(conf.scenario.detectionQuality - 1, DETECTION_QUALITY_MIN.toInt()),
            ).toScenarioConfig(conf)
        }
    }

    /** Add one to the detection quality */
    fun increaseDetectionQuality() {
        configuredScenario.value?.let { conf ->
            configuredScenario.value = conf.scenario.copy(
                detectionQuality = min(conf.scenario.detectionQuality + 1, DETECTION_QUALITY_MAX.toInt()),
            ).toScenarioConfig(conf)
        }
    }

    /**
     * Set the detection quality for the scenario.
     * @param quality the value from the seekbar.
     */
    fun setDetectionQuality(quality: Int) {
        configuredScenario.value?.let { conf ->
            configuredScenario.value = conf.scenario.copy(detectionQuality = quality).toScenarioConfig(conf)
        }
    }

    /** Toggle the end condition operator between AND and OR. */
    fun setConditionOperator(operatorItem: DropdownItem) {
        configuredScenario.value?.let { conf ->
            val operator = when (operatorItem) {
                conditionAndItem -> AND
                conditionOrItem -> OR
                else -> return
            }

            configuredScenario.value = conf.scenario.copy(endConditionOperator = operator).toScenarioConfig(conf)
        }
    }

    /** @return a new empty end condition. */
    fun createNewEndCondition() =
        EndCondition(scenarioId = configuredScenario.value?.scenario?.id ?: 0)

    /**
     * Add a new end condition to the scenario.
     * @param endCondition the end condition to be added.
     */
    fun addEndCondition(endCondition: EndCondition) {
        if (endCondition.id != 0L) return

        configuredScenario.value?.let { conf ->
            val newList = conf.endConditions.toMutableList().apply { add(endCondition) }
            configuredScenario.value = newList.toScenarioConfig(conf)
        }
    }

    /**
     * Update an end condition from the scenario.
     * @param endCondition the end condition to be updated.
     */
    fun updateEndCondition(endCondition: EndCondition, index: Int) {
        configuredScenario.value?.let { conf ->
            val newList = conf.endConditions.toMutableList().apply { set(index, endCondition) }
            configuredScenario.value = newList.toScenarioConfig(conf)
        }
    }

    /**
     * Delete a end condition from the scenario.
     * @param endCondition the end condition to be removed.
     */
    fun deleteEndCondition(endCondition: EndCondition) {
        configuredScenario.value?.let { conf ->
            val newList = conf.endConditions.toMutableList().apply { remove(endCondition) }
            configuredScenario.value = newList.toScenarioConfig(conf)
        }
    }

    private fun Scenario.toScenarioConfig(configuredScenario: ConfiguredScenario): ConfiguredScenario =
        configuredScenario.copy(scenario = this)

    private fun List<EndCondition>.toScenarioConfig(configuredScenario: ConfiguredScenario): ConfiguredScenario =
        configuredScenario.copy(endConditions = this)
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