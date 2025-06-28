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
package com.buzbuz.smartautoclicker.scenarios.list

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository

import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioBackupSelection
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState
import com.buzbuz.smartautoclicker.scenarios.list.model.isEmpty
import com.buzbuz.smartautoclicker.scenarios.list.model.toggleAllScenarioSelectionForBackup
import com.buzbuz.smartautoclicker.scenarios.list.model.toggleScenarioSelectionForBackup
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap
import com.buzbuz.smartautoclicker.scenarios.list.sort.ScenarioSortConfigRepository
import com.buzbuz.smartautoclicker.scenarios.list.sort.ScenarioSortType

import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import javax.inject.Inject

@HiltViewModel
class ScenarioListViewModel @Inject constructor(
    private val filteredScenarioListUseCase: FilteredScenarioListUseCase,
    private val sortConfigRepository: ScenarioSortConfigRepository,
    private val bitmapRepository: BitmapRepository,
    private val smartRepository: IRepository,
    private val dumbRepository: IDumbRepository,
) : ViewModel() {

    /** Current state type of the ui. */
    private val uiStateType = MutableStateFlow(ScenarioListUiState.Type.SELECTION)

    /** Set of scenario with their items expanded. */
    private val expandedItems = MutableStateFlow(ScenarioExpandedSelection())
    /** Set of scenario identifier selected for a backup. */
    private val selectedForBackup = MutableStateFlow(ScenarioBackupSelection())

    val uiState: StateFlow<ScenarioListUiState?> = combine(
        uiStateType,
        filteredScenarioListUseCase.orderedItems,
        selectedForBackup,
        expandedItems,
    ) { stateType, items, backupSelection, expanded,  ->
        ScenarioListUiState(
            type = stateType,
            menuUiState = stateType.toMenuUiState(items, backupSelection),
            listContent =
                if (stateType != ScenarioListUiState.Type.EXPORT) items.updateExpanded(expanded)
                else items.filterForBackupSelection(backupSelection),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val needsConditionMigration: Flow<Boolean> =
        smartRepository.legacyConditionsCount.map { it != 0 }

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
        filteredScenarioListUseCase.updateSearchQuery(query)
    }

    fun updateSortType(type: ScenarioSortType) {
        viewModelScope.launch {
            sortConfigRepository.setSortType(type)
        }
    }

    fun updateSortOrder(isChecked: Boolean) {
        viewModelScope.launch {
            sortConfigRepository.setSortOrder(isChecked)
        }
    }

    fun updateDumbVisible(show: Boolean) {
        viewModelScope.launch {
            sortConfigRepository.setShowDumb(show)
        }
    }

    fun updateSmartVisible(show: Boolean) {
        viewModelScope.launch {
            sortConfigRepository.setShowSmart(show)
        }
    }

    fun refreshScenarioList() {
        viewModelScope.launch {
            filteredScenarioListUseCase.refresh()
        }
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

    fun expandCollapseItem(item: ScenarioListUiState.Item) {
        if (item !is ScenarioListUiState.Item.ScenarioItem.Valid) return

        expandedItems.value = expandedItems.value.let { oldSelection ->
            when (item) {
                is ScenarioListUiState.Item.ScenarioItem.Valid.Smart -> {
                    oldSelection.copy(
                        smartSelection = oldSelection.smartSelection
                            .toMutableSet()
                            .toggleExpandedSelection(item.getScenarioId())
                    )
                }

                is ScenarioListUiState.Item.ScenarioItem.Valid.Dumb -> {
                    oldSelection.copy(
                        dumbSelection = oldSelection.dumbSelection
                            .toMutableSet()
                            .toggleExpandedSelection(item.getScenarioId())
                    )
                }
            }
        }
    }

    /**
     * Delete a click scenario.
     *
     * This will also delete all child entities associated with the scenario.
     *
     * @param item the scenario to be deleted.
     */
    fun deleteScenario(item: ScenarioListUiState.Item.ScenarioItem) {
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
        getImageConditionBitmap(bitmapRepository, condition, onBitmapLoaded)

    private fun ScenarioListUiState.Type.toMenuUiState(
        scenarioItems: List<ScenarioListUiState.Item>,
        backupSelection: ScenarioBackupSelection,
    ): ScenarioListUiState.Menu = when (this) {
        ScenarioListUiState.Type.SEARCH -> ScenarioListUiState.Menu.Search
        ScenarioListUiState.Type.EXPORT -> ScenarioListUiState.Menu.Export(
            canExport = !backupSelection.isEmpty(),
        )
        ScenarioListUiState.Type.SELECTION -> ScenarioListUiState.Menu.Selection(
            searchEnabled = scenarioItems.isNotEmpty(),
            exportEnabled = scenarioItems.firstOrNull { it is ScenarioListUiState.Item.ScenarioItem.Valid } != null,
        )
    }

    private fun List<ScenarioListUiState.Item>.filterForBackupSelection(
        backupSelection: ScenarioBackupSelection,
    ) : List<ScenarioListUiState.Item> = mapNotNull { item ->
        when (item) {
            is ScenarioListUiState.Item.SortItem -> item
            is ScenarioListUiState.Item.ScenarioItem.Valid.Dumb -> item.copy(
                showExportCheckbox = true,
                checkedForExport = backupSelection.dumbSelection.contains(item.scenario.id.databaseId)
            )
            is ScenarioListUiState.Item.ScenarioItem.Valid.Smart -> item.copy(
                showExportCheckbox = true,
                checkedForExport = backupSelection.smartSelection.contains(item.scenario.id.databaseId)
            )
            else -> null
        }
    }

    private fun List<ScenarioListUiState.Item>.updateExpanded(
        expanded: ScenarioExpandedSelection,
    ) : List<ScenarioListUiState.Item> = map { item ->
        when (item) {
            is ScenarioListUiState.Item.ScenarioItem.Valid.Dumb ->
                item.copy(expanded = expanded.dumbSelection.contains(item.getScenarioId()))
            is ScenarioListUiState.Item.ScenarioItem.Valid.Smart ->
                item.copy(expanded = expanded.smartSelection.contains(item.getScenarioId()))
            else -> item
        }
    }

    private fun MutableSet<Long>.toggleExpandedSelection(id: Long): MutableSet<Long> {
        if (!contains(id)) add(id)
        else remove(id)

        return this
    }
}

data class ScenarioExpandedSelection(
    val dumbSelection: Set<Long> = mutableSetOf(),
    val smartSelection: Set<Long> = mutableSetOf(),
)
