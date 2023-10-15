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
package com.buzbuz.smartautoclicker.activity.creation

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.base.identifier.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class ScenarioCreationViewModel(application: Application) : AndroidViewModel(application) {

    private val dumbRepository = DumbRepository.getRepository(application)
    private val repository = Repository.getRepository(application)

    private val billingRepository = IBillingRepository.getRepository(application)

    /** Tells if the limitation in smart scenario count have been reached. */
    private val isSmartScenarioLimitReached: Flow<Boolean> = billingRepository.isProModePurchased
        .combine(repository.scenarios) { isProModePurchased, scenarios ->
            !isProModePurchased && scenarios.size >= ProModeAdvantage.Limitation.SMART_SCENARIO_COUNT_LIMIT.limit
        }

    private val _name: MutableStateFlow<String?> =
        MutableStateFlow(application.getString(R.string.default_scenario_name))
    val name: Flow<String> = _name
        .map { it ?: "" }
        .take(1)
    val nameError: Flow<Boolean> = _name
        .map { it.isNullOrEmpty() }

    private val _selectedType: MutableStateFlow<ScenarioTypeSelection> =
        MutableStateFlow(ScenarioTypeSelection.SMART)
    val scenarioTypeSelectionState: Flow<ScenarioTypeSelectionState> =
        combine(_selectedType, billingRepository.isProModePurchased, isSmartScenarioLimitReached) { selectedType, isProMode, limitIsReached ->
            when {
                limitIsReached -> ScenarioTypeSelectionState(
                    dumbItem = ScenarioTypeItem.Dumb,
                    smartItem = ScenarioTypeItem.Smart(isProModeEnabled = isProMode),
                    smartItemEnabled = false,
                    selectedItem = ScenarioTypeSelection.DUMB,
                )

                else -> ScenarioTypeSelectionState(
                    dumbItem = ScenarioTypeItem.Dumb,
                    smartItem = ScenarioTypeItem.Smart(isProModeEnabled = isProMode),
                    smartItemEnabled = true,
                    selectedItem = selectedType,
                )
            }
        }

    private val canBeCreated: Flow<Boolean> = _name.map { name -> !name.isNullOrEmpty() }
    private val _creationState: MutableStateFlow<CreationState> =
        MutableStateFlow(CreationState.CONFIGURING)
    val creationState: Flow<CreationState> = _creationState.combine(canBeCreated) { state, valid ->
        if (state == CreationState.CONFIGURING && !valid) CreationState.CONFIGURING_INVALID
        else state
    }

    fun setName(newName: String?) {
        _name.value = newName
    }

    fun setSelectedType(type: ScenarioTypeSelection) {
        _selectedType.value = type
    }

    fun createScenario(context: Context) {
        if (isInvalidForCreation() || _creationState.value != CreationState.CONFIGURING) return

        _creationState.value = CreationState.CREATING
        viewModelScope.launch(Dispatchers.IO) {
            when (_selectedType.value) {
                ScenarioTypeSelection.DUMB -> createDumbScenario()
                ScenarioTypeSelection.SMART -> createSmartScenario(context)
            }
            _creationState.value = CreationState.SAVED
        }
    }

    fun onScenarioCountReachedAddCopyClicked(context: Context) {
        billingRepository.startBillingActivity(
            context,
            ProModeAdvantage.Limitation.SMART_SCENARIO_COUNT_LIMIT,
        )
    }

    private suspend fun createDumbScenario() {
        dumbRepository.addDumbScenario(
            DumbScenario(
                id = Identifier(databaseId = DATABASE_ID_INSERTION, domainId = 0L),
                name = _name.value!!,
                dumbActions = emptyList(),
                repeatCount = 1,
                isRepeatInfinite = false,
                maxDurationMin = 1,
                isDurationInfinite = true,
                randomize = false,
            )
        )
    }

    private suspend fun createSmartScenario(context: Context) {
        repository.addScenario(
            Scenario(
                id = Identifier(databaseId = DATABASE_ID_INSERTION, domainId = 0L),
                name = _name.value!!,
                detectionQuality = context.resources.getInteger(R.integer.default_detection_quality),
                endConditionOperator = OR,
            )
        )
    }

    private fun isInvalidForCreation(): Boolean = _name.value.isNullOrEmpty()
}


data class ScenarioTypeSelectionState(
    val dumbItem: ScenarioTypeItem.Dumb,
    val smartItem: ScenarioTypeItem.Smart,
    val selectedItem: ScenarioTypeSelection,
    val smartItemEnabled: Boolean,
)
sealed class ScenarioTypeItem(val titleRes: Int,  val iconRes: Int, val descriptionText: Int) {

    data object Dumb: ScenarioTypeItem(
        titleRes = R.string.item_title_dumb_scenario,
        iconRes = R.drawable.ic_dumb,
        descriptionText = R.string.item_desc_dumb_scenario,
    )

    data class Smart(val isProModeEnabled: Boolean): ScenarioTypeItem(
        titleRes = R.string.item_title_smart_scenario,
        iconRes = R.drawable.ic_smart,
        descriptionText =
            if (isProModeEnabled) R.string.item_desc_smart_scenario_pro_mode
            else R.string.item_desc_dumb_scenario,
    )
}
enum class ScenarioTypeSelection {
    DUMB,
    SMART,
}

enum class CreationState {
    CONFIGURING_INVALID,
    CONFIGURING,
    CREATING,
    SAVED,
}