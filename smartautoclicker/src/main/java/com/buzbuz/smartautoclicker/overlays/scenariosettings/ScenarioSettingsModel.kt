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
package com.buzbuz.smartautoclicker.overlays.scenariosettings

import android.content.Context

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.database.Repository
import com.buzbuz.smartautoclicker.database.domain.*
import com.buzbuz.smartautoclicker.detection.DETECTION_QUALITY_MAX
import com.buzbuz.smartautoclicker.detection.DETECTION_QUALITY_MIN
import com.buzbuz.smartautoclicker.overlays.eventlist.EventListDialog
import com.buzbuz.smartautoclicker.overlays.eventlist.Mode

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * View model for the [ScenarioSettingsDialog].
 *
 * @param context the Android context.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioSettingsModel(context: Context) : OverlayViewModel(context) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(context)

    /** The scenario configured. */
    private val configuredScenario = MutableStateFlow<Scenario?>(null)
    /** The list of end condition configured. */
    private val configuredEndConditions = MutableStateFlow<List<EndCondition>>(emptyList())

    /** The quality of the detection. */
    val detectionQuality = configuredScenario.map { it?.detectionQuality }
    /** The operator applied to the end conditions. */
    val endConditionOperator = configuredScenario.map { it?.endConditionOperator }
    /** The end conditions for the configured scenario. */
    val endConditions = configuredEndConditions.map { endConditions ->
        buildList {
            add(EndConditionListItem.AddEndConditionItem)
            endConditions.forEach { add(EndConditionListItem.EndConditionItem(it)) }
        }
    }

    /**
     * Set the scenario to be configured.
     * @param id the unique identifier of the scenario.
     */
    fun setScenario(id: Long) {
        viewModelScope.launch {
            val scenarioWithEndConditions = repository.getScenarioWithEndConditions(id)
            configuredScenario.value = scenarioWithEndConditions.first

        }
    }

    /**
     * Set the detection quality for the scenario.
     * @param seekbarValue the value from the seekbar, will be corrected to use normal range.
     */
    fun setDetectionQuality(seekbarValue: Int) {
        configuredScenario.value?.let { scenario ->
            configuredScenario.value = scenario.copy(detectionQuality = seekbarValue + DETECTION_QUALITY_MIN.toInt())
        } ?: throw IllegalStateException("Can't set detection quality, scenario is null!")
    }

    /** Toggle the end condition operator between AND and OR. */
    fun toggleEndConditionOperator() {
        configuredScenario.value?.let { scenario ->
            configuredScenario.value = scenario.copy(
                endConditionOperator = if (scenario.endConditionOperator == AND) OR else AND
            )
        } ?: throw IllegalStateException("Can't set end condition operator, scenario is null!")
    }
}

/** Items displayed in the end condition list. */
sealed class EndConditionListItem {
    /** The add end condition item. */
    object AddEndConditionItem : EndConditionListItem()
    /** Item representing a end condition. */
    data class EndConditionItem(val endCondition: EndCondition) : EndConditionListItem()
}

/** The maximum value for the seek bar. */
const val SEEK_BAR_QUALITY_MAX = (DETECTION_QUALITY_MAX - DETECTION_QUALITY_MIN).toInt()