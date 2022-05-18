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
package com.buzbuz.smartautoclicker.activity

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.IntRange

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.SmartAutoClickerService
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.Scenario
import com.buzbuz.smartautoclicker.overlays.utils.newDefaultScenario

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** AndroidViewModel for create/delete/list click scenarios from an LifecycleOwner. */
class ScenarioViewModel(application: Application) : AndroidViewModel(application) {

    /** The repository providing access to the database. */
    private val repository = Repository.getRepository(application)
    /** Callback upon the availability of the [SmartAutoClickerService]. */
    private val serviceConnection: (SmartAutoClickerService.LocalService?) -> Unit = { localService ->
        clickerService = localService
    }

    /**
     * Reference on the [SmartAutoClickerService].
     * Will be not null only if the Accessibility Service is enabled.
     */
    private var clickerService: SmartAutoClickerService.LocalService? = null

    /** Set of scenario identifier selected for a backup. */
    private val selectedForBackup = MutableStateFlow(emptySet<Long>())

    /** Backing property for [menuState]. */
    private val _menuState = MutableStateFlow(MenuState.SELECTION)
    /** Current menu state. */
    val menuState: StateFlow<MenuState> = _menuState
    /** Current menu UI state. */
    val menuUiState: Flow<MenuUiState> = _menuState
        .combine(selectedForBackup) { menuState, selection ->
            when (menuState) {
                MenuState.SEARCH -> MenuUiState(
                    state = menuState,
                    searchVisibility = false,
                    importBackupVisibility = false,
                    cancelVisibility = false,
                    selectAllVisibility = false,
                    createBackupVisibility = false,
                    createBackupEnabled = false,
                )
                MenuState.EXPORT -> MenuUiState(
                    state = menuState,
                    searchVisibility = false,
                    importBackupVisibility = false,
                    cancelVisibility = true,
                    selectAllVisibility = true,
                    createBackupVisibility = true,
                    createBackupEnabled = selection.isNotEmpty(),
                    createBackupAlpha = if (selection.isNotEmpty()) 255 else 127
                )
                MenuState.SELECTION -> MenuUiState(
                    state = menuState,
                    searchVisibility = true,
                    importBackupVisibility = true,
                    cancelVisibility = false,
                    selectAllVisibility = false,
                    createBackupVisibility = true,
                    createBackupEnabled = true,
                )
            }
        }

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)
    /** Flow upon the list of scenarios. */
    val scenarioList: StateFlow<List<ScenarioItem>> =
        combine(repository.scenarios, searchQuery, _menuState, selectedForBackup) { scenarios, query, menuState, backupSelection ->
            scenarios.mapNotNull { scenario ->
                if (query.isNullOrEmpty() || scenario.name.contains(query.toString(), true)) {
                    ScenarioItem(
                        scenario = scenario,
                        exportMode = menuState == MenuState.EXPORT,
                        checkedForExport = backupSelection.contains(scenario.id)
                    )
                } else {
                    null
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    init {
        SmartAutoClickerService.getLocalService(serviceConnection)
    }

    override fun onCleared() {
        SmartAutoClickerService.getLocalService(null)
        super.onCleared()
    }

    /**
     * Tells if the overlay permission is granted for this application.
     *
     * @return true if the permission is granted, false if not.
     */
    fun isOverlayPermissionValid(): Boolean = Settings.canDrawOverlays(getApplication())

    /**
     * Tells if the Accessibility Service of this application is started.
     *
     * @return true if the service is started, false if not.
     */
    fun isAccessibilityPermissionValid(): Boolean = clickerService != null

    /**
     * Tells if all application permission are granted.
     *
     * @return true if they are all granted, false if at least one is not.
     */
    fun arePermissionsGranted(): Boolean = isOverlayPermissionValid() && isAccessibilityPermissionValid()

    /**
     * Create a new click scenario.
     *
     * @param context the Android context.
     * @param name the name of this new scenario.
     */
    fun createScenario(context: Context, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addScenario(newDefaultScenario(context, name))
        }
    }

    /**
     * Rename the selected scenario.
     *
     * @param scenario the scenario to be renamed
     * @param name the new name of the scenario
     */
    fun renameScenario(scenario: Scenario, name: String) {
        scenario.name = name
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateScenario(scenario)
        }
    }

    /**
     * Delete a click scenario.
     *
     * This will also delete all clicks associated with the scenario.
     *
     * @param scenario the scenario to be deleted.
     */
    fun deleteScenario(scenario: Scenario) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteScenario(scenario) }
    }

    /**
     * Change the menu state.
     * @param menuState the new state.
     */
    fun setMenuState(menuState: MenuState) {
        _menuState.value = menuState
        selectedForBackup.value = selectedForBackup.value.toMutableSet().apply { clear() }
    }

    /**
     * Toggle the selected for backup state of a scenario.
     * @param scenarioId the identifier of the scenario to be toggled.
     */
    fun toggleScenarioSelectionForBackup(scenarioId: Long) {
        val newSelection = selectedForBackup.value.toMutableSet().apply {
            if (contains(scenarioId)) remove(scenarioId)
            else add(scenarioId)
        }
        selectedForBackup.value = newSelection
    }

    /** Toggle the selected for backup state value for all scenario. */
    fun toggleAllScenarioSelectionForBackup() {
        if (scenarioList.value.size == selectedForBackup.value.size) {
            selectedForBackup.value = emptySet()
        } else {
            selectedForBackup.value = scenarioList.value.map { it.scenario.id }.toSet()
        }
    }

    /** @return the list of selected scenario identifiers. */
    fun getScenariosSelectedForBackup(): Collection<Long> = selectedForBackup.value.toList()

    /**
     * Start the overlay UI and instantiates the detection objects for a given scenario.
     *
     * This requires the media projection permission code and its data intent, they both can be retrieved using the
     * results of the activity intent provided by
     * [android.media.projection.MediaProjectionManager.createScreenCaptureIntent] (this Intent shows the dialog
     * warning about screen recording privacy). Any attempt to call this method without the correct screen capture
     * intent result will leads to a crash.
     *
     * @param resultCode the result code provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param data the data intent provided by the screen capture intent activity result callback
     * [android.app.Activity.onActivityResult]
     * @param scenario the identifier of the scenario of clicks to be used for detection.
     */
    fun loadScenario(resultCode: Int, data: Intent, scenario: Scenario) {
        clickerService?.start(resultCode, data, scenario)
    }

    /** Stop the overlay UI and release all associated resources. */
    fun stopScenario() {
        clickerService?.stop()
    }

    /**
     * Update the action search query.
     * @param query the new query.
     */
    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }
}

/** Possible states for the action menu of the ScenarioListFragment. */
enum class MenuState {
    /** The user can select a scenario to be played/edited.*/
    SELECTION,
    /** The user is searching for a scenario. */
    SEARCH,
    /** The user is selecting the scenarios to export. */
    EXPORT,
}

/**
 * Ui state of the action menu.
 *
 * @param state the current state.
 * @param searchVisibility the visibility of the search item.
 * @param importBackupVisibility the visibility of the import item.
 * @param selectAllVisibility the visibility of the select all item.
 * @param cancelVisibility the visibility of the cancel item.
 * @param createBackupVisibility the visibility of the export item.
 * @param createBackupEnabled true if the user can click the export item, false if not.
 * @param createBackupAlpha the alpha to apply to the export item. 0 is transparent, 255 is opaque.
 */
data class MenuUiState(
    val state: MenuState,
    val searchVisibility: Boolean,
    val importBackupVisibility: Boolean,
    val selectAllVisibility: Boolean,
    val cancelVisibility: Boolean,
    val createBackupVisibility: Boolean,
    val createBackupEnabled: Boolean,
    @IntRange(from = 0, to = 255) val createBackupAlpha: Int = 255,
)

/**
 * Item representing a scenario on the list.
 *
 * @param scenario the scenario represented by the item.
 * @param exportMode true if the item should be displayed in export scenario mode.
 * @param checkedForExport true if the scenario is selected for export, false if not.
 */
data class ScenarioItem(
    val scenario: Scenario,
    val exportMode: Boolean,
    val checkedForExport: Boolean,
)
