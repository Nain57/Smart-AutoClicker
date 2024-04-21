/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.config

import android.content.Context

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.processing.domain.DETECTION_QUALITY_MAX
import com.buzbuz.smartautoclicker.core.processing.domain.DETECTION_QUALITY_MIN
import com.buzbuz.smartautoclicker.feature.smart.config.R

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

import kotlin.math.max
import kotlin.math.min

/** View model for the [ScenarioConfigContent]. */
class ScenarioConfigViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
    private val billingRepository: IBillingRepository,
) : ViewModel() {

    /** Currently configured scenario. */
    private val configuredScenario = editionRepository.editionState.scenarioState
        .mapNotNull { it.value }

    /** The event name value currently edited by the user. */
    val scenarioName: Flow<String> = configuredScenario
        .map { it.name }
        .filterNotNull()
        .take(1)
    /** Tells if the scenario name is valid or not. */
    val scenarioNameError: Flow<Boolean> = configuredScenario
        .map { it.name.isEmpty() }

    private val enabledRandomization = DropdownItem(
        title = R.string.dropdown_item_title_anti_detection_enabled,
        helperText = R.string.dropdown_helper_text_anti_detection_enabled,
    )
    private val disableRandomization = DropdownItem(
        title = R.string.dropdown_item_title_anti_detection_disabled,
        helperText = R.string.dropdown_helper_text_anti_detection_disabled,
    )
    val randomizationDropdownState: Flow<RandomizationDropdownUiState> = billingRepository.isProModePurchased
        .map { isProModePurchased ->
            RandomizationDropdownUiState(
                items = listOf(enabledRandomization, disableRandomization),
                enabled = isProModePurchased,
                disabledIcon = R.drawable.ic_pro_small,
            )
        }

    /** The randomization value for the scenario. */
    val randomization: Flow<DropdownItem> = configuredScenario
        .map {
            when (it.randomize) {
                true -> enabledRandomization
                false -> disableRandomization
            }
        }
        .filterNotNull()

    /** The quality of the detection. */
    val detectionQuality: Flow<Int?> = configuredScenario
        .map { it.detectionQuality }

    /** Tells if the pro mode has been purchased by the user. */
    val isProModePurchased: Flow<Boolean> = billingRepository.isProModePurchased
    /** Tells if the pro mode billing flow is being displayed. */
    val isBillingFlowDisplayed: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    /** Set a new name for the scenario. */
    fun setScenarioName(name: String) {
        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(scenario.copy(name = name))
            }
        }
    }

    /** Toggle the randomization value. */
    fun setRandomization(randomizationItem: DropdownItem) {
        editionRepository.editionState.getScenario()?.let { scenario ->
            val value = when (randomizationItem) {
                enabledRandomization -> true
                disableRandomization -> false
                else -> return
            }

            viewModelScope.launch {
                editionRepository.updateEditedScenario(scenario.copy(randomize = value))
            }
        }
    }

    /** Remove one to the detection quality */
    fun decreaseDetectionQuality() {
        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(
                    scenario.copy(
                        detectionQuality = max(scenario.detectionQuality - 1, DETECTION_QUALITY_MIN.toInt())
                    )
                )
            }
        }
    }

    /** Add one to the detection quality */
    fun increaseDetectionQuality() {
        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(
                    scenario.copy(
                        detectionQuality = min(scenario.detectionQuality + 1, DETECTION_QUALITY_MAX.toInt())
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
        editionRepository.editionState.getScenario()?.let { scenario ->
            viewModelScope.launch {
                editionRepository.updateEditedScenario(scenario.copy(detectionQuality = quality))
            }
        }
    }

    fun onAntiDetectionClickedWithoutProMode(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Feature.SCENARIO_ANTI_DETECTION)
    }

    fun onDetectionQualityClickedWithoutProMode(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Feature.SCENARIO_DETECTION_QUALITY)
    }
}

data class RandomizationDropdownUiState(
    val items: List<DropdownItem>,
    val enabled: Boolean = true,
    @DrawableRes val disabledIcon: Int? = null,
)

/** The minimum value for the seek bar. */
const val SLIDER_QUALITY_MIN = DETECTION_QUALITY_MIN.toFloat()
/** The maximum value for the seek bar. */
const val SLIDER_QUALITY_MAX = DETECTION_QUALITY_MAX.toFloat()