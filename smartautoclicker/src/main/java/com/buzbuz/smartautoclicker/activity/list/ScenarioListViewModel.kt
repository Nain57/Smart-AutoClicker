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
package com.buzbuz.smartautoclicker.activity.list

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Repeatable
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import javax.inject.Inject

@HiltViewModel
class ScenarioListViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val smartRepository: IRepository,
    private val dumbRepository: IDumbRepository,
    private val billingRepository: IBillingRepository,
) : ViewModel() {

    /** Current state type of the ui. */
    private val uiStateType = MutableStateFlow(ScenarioListUiState.Type.SELECTION)

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)
    /** Dumb & Smart scenario together. */
    private val allScenarios: Flow<List<ScenarioListUiState.Item>> =
        combine(dumbRepository.dumbScenarios, smartRepository.scenarios) { dumbList, smartList ->
            mutableListOf<ScenarioListUiState.Item>().apply {
                addAll(dumbList.map { it.toItem(context) })
                addAll(smartList.map { it.toItem() })
            }.sortedBy { it.displayName }
        }
    /** Flow upon the list of Dumb & Smart scenarios, filtered with the search query. */
    private val filteredScenarios: Flow<List<ScenarioListUiState.Item>> = allScenarios
        .combine(searchQuery) { scenarios, query ->
            scenarios.mapNotNull { scenario ->
                if (query.isNullOrEmpty()) return@mapNotNull scenario
                if (scenario.displayName.contains(query.toString(), true)) scenario else null
            }
        }

    /** Set of scenario identifier selected for a backup. */
    private val selectedForBackup = MutableStateFlow(ScenarioBackupSelection())

    val uiState: StateFlow<ScenarioListUiState?> = combine(
        uiStateType,
        filteredScenarios,
        selectedForBackup,
        billingRepository.isProModePurchased,
    ) { stateType, scenarios, backupSelection, isProMode ->
        ScenarioListUiState(
            type = stateType,
            menuUiState = stateType.toMenuUiState(scenarios, backupSelection, isProMode),
            listContent =
                if (stateType != ScenarioListUiState.Type.EXPORT) scenarios
                else scenarios.filterForBackupSelection(backupSelection),
            isProModePurchased = isProMode,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    /**
     * Change the ui state type.
     * @param state the new state.
     */
    fun setUiState(state: ScenarioListUiState.Type) {
        uiStateType.value = state
        selectedForBackup.value = selectedForBackup.value.copy(
            dumbSelection = emptySet(),
            smartSelection = emptySet(),
        )
    }

    /**
     * Update the action search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    /** @return the list of selected dumb scenario identifiers. */
    fun getDumbScenariosSelectedForBackup(): Collection<Long> =
        selectedForBackup.value.dumbSelection.toList()

    /** @return the list of selected smart scenario identifiers. */
    fun getSmartScenariosSelectedForBackup(): Collection<Long> =
        selectedForBackup.value.smartSelection.toList()

    /**
     * Toggle the selected for backup state of a scenario.
     * @param scenario the scenario to be toggled.
     */
    fun toggleScenarioSelectionForBackup(scenario: ScenarioListUiState.Item) {
        selectedForBackup.value.toggleScenarioSelectionForBackup(scenario)?.let {
            selectedForBackup.value = it
        }
    }

    /** Toggle the selected for backup state value for all scenario. */
    fun toggleAllScenarioSelectionForBackup() {
        selectedForBackup.value = selectedForBackup.value.toggleAllScenarioSelectionForBackup(
            uiState.value?.listContent ?: emptyList()
        )
    }

    /**
     * Delete a click scenario.
     *
     * This will also delete all child entities associated with the scenario.
     *
     * @param item the scenario to be deleted.
     */
    fun deleteScenario(item: ScenarioListUiState.Item) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val scenario = item.scenario) {
                is DumbScenario -> dumbRepository.deleteDumbScenario(scenario)
                is Scenario -> smartRepository.deleteScenario(scenario.id)
            }
        }
    }

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: ImageCondition, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(smartRepository, condition, onBitmapLoaded)

    fun onExportClickedWithoutProMode(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Feature.BACKUP_EXPORT)
    }

    fun onImportClickedWithoutProMode(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Feature.BACKUP_IMPORT)
    }

    private fun ScenarioListUiState.Type.toMenuUiState(
        scenarioItems: List<ScenarioListUiState.Item>,
        backupSelection: ScenarioBackupSelection,
        isProModePurchased: Boolean,
    ): ScenarioListUiState.Menu = when (this) {
        ScenarioListUiState.Type.SEARCH -> ScenarioListUiState.Menu.Search
        ScenarioListUiState.Type.EXPORT -> ScenarioListUiState.Menu.Export(!backupSelection.isEmpty())
        ScenarioListUiState.Type.SELECTION -> ScenarioListUiState.Menu.Selection(
            searchEnabled = scenarioItems.isNotEmpty(),
            exportEnabled = scenarioItems.firstOrNull { it is ScenarioListUiState.Item.Valid } != null,
            isProMode = isProModePurchased,
        )
    }

    private suspend fun Scenario.toItem(): ScenarioListUiState.Item =
        if (eventCount == 0) ScenarioListUiState.Item.Empty.Smart(this)
        else ScenarioListUiState.Item.Valid.Smart(
            scenario = this,
            eventsItems = smartRepository.getImageEvents(id.databaseId).map { event ->
                ScenarioListUiState.Item.Valid.Smart.EventItem(
                    id = event.id.databaseId,
                    eventName = event.name,
                    actionsCount = event.actions.size,
                    conditionsCount = event.conditions.size,
                    firstCondition = if (event.conditions.isNotEmpty()) event.conditions.first() else null,
                )
            },
            triggerEventCount = smartRepository.getTriggerEvents(id.databaseId).size,
            detectionQuality = detectionQuality,
        )

    private fun DumbScenario.toItem(context: Context): ScenarioListUiState.Item =
        if (dumbActions.isEmpty()) ScenarioListUiState.Item.Empty.Dumb(this)
        else ScenarioListUiState.Item.Valid.Dumb(
            scenario = this,
            clickCount = dumbActions.count { it is DumbAction.DumbClick },
            swipeCount = dumbActions.count { it is DumbAction.DumbSwipe },
            pauseCount = dumbActions.count { it is DumbAction.DumbPause },
            repeatText = getRepeatDisplayText(context),
            maxDurationText = getMaxDurationDisplayText(context),
        )

    private fun Repeatable.getRepeatDisplayText(context: Context): String =
        if (isRepeatInfinite) context.getString(R.string.item_desc_dumb_scenario_repeat_infinite)
        else context.getString(R.string.item_desc_dumb_scenario_repeat_count, repeatCount)

    private fun DumbScenario.getMaxDurationDisplayText(context: Context): String =
        if (isDurationInfinite) context.getString(R.string.item_desc_dumb_scenario_max_duration_infinite)
        else context.getString(
            R.string.item_desc_dumb_scenario_max_duration,
            formatDuration(maxDurationMin.minutes.inWholeMilliseconds),
        )

    private fun List<ScenarioListUiState.Item>.filterForBackupSelection(
        backupSelection: ScenarioBackupSelection,
    ) : List<ScenarioListUiState.Item> = mapNotNull { item ->
        when (item) {
            is ScenarioListUiState.Item.Valid.Dumb -> item.copy(
                showExportCheckbox = true,
                checkedForExport = backupSelection.dumbSelection.contains(item.scenario.id.databaseId)
            )
            is ScenarioListUiState.Item.Valid.Smart -> item.copy(
                showExportCheckbox = true,
                checkedForExport = backupSelection.smartSelection.contains(item.scenario.id.databaseId)
            )
            else -> null
        }
    }
}