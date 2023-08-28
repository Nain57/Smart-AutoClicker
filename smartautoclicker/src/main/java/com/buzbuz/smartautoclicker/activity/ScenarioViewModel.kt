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
package com.buzbuz.smartautoclicker.activity

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings

import androidx.annotation.IntRange
import androidx.core.content.PermissionChecker
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.SmartAutoClickerService
import com.buzbuz.smartautoclicker.activity.ScenarioListFragmentUiState.EventItem
import com.buzbuz.smartautoclicker.activity.ScenarioListFragmentUiState.Menu
import com.buzbuz.smartautoclicker.activity.ScenarioListFragmentUiState.ScenarioListItem
import com.buzbuz.smartautoclicker.activity.ScenarioListFragmentUiState.Type
import com.buzbuz.smartautoclicker.feature.billing.domain.BillingRepository
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.model.DATABASE_ID_INSERTION
import com.buzbuz.smartautoclicker.core.domain.model.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_DISABLED_ITEM_INT
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_ENABLED_ITEM_INT

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** AndroidViewModel for create/delete/list click scenarios from an LifecycleOwner. */
class ScenarioViewModel(application: Application) : AndroidViewModel(application) {

    /** The repository providing access to the database. */
    private val repository = Repository.getRepository(application)
    /** The repository for the pro mode billing. */
    private val billingRepository: BillingRepository = IBillingRepository.getRepository(application.applicationContext)

    /** Callback upon the availability of the [SmartAutoClickerService]. */
    private val serviceConnection: (SmartAutoClickerService.LocalService?) -> Unit = { localService ->
        clickerService = localService
    }

    /**
     * Reference on the [SmartAutoClickerService].
     * Will be not null only if the Accessibility Service is enabled.
     */
    private var clickerService: SmartAutoClickerService.LocalService? = null

    /** Current state type of the ui. */
    private val uiStateType = MutableStateFlow(Type.SELECTION)

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)
    /** Flow upon the list of scenarios, filtered with the search query. */
    private val filteredScenarios: Flow<List<Scenario>> = repository.scenarios
        .combine(searchQuery) { scenarios, query ->
            scenarios.mapNotNull { scenario ->
                if (query.isNullOrEmpty() || scenario.name.contains(query.toString(), true)) scenario
                else null
            }
        }

    /** Set of scenario identifier selected for a backup. */
    private val selectedForBackup = MutableStateFlow(emptySet<Long>())

    /** Tells if the limitation in scenario count have been reached. */
    private val isScenarioLimitReached: Flow<Boolean> = billingRepository.isProModePurchased
        .combine(repository.scenarios) { isProModePurchased, scenarios ->
            !isProModePurchased && scenarios.size >= ProModeAdvantage.Limitation.SCENARIO_COUNT_LIMIT.limit
        }

    val uiState: StateFlow<ScenarioListFragmentUiState?> = combine(
        uiStateType,
        filteredScenarios,
        selectedForBackup,
        isScenarioLimitReached,
        billingRepository.isProModePurchased,
    ) { stateType, scenarios, backupSelection, isLimitReached, isProMode ->

        val scenarioList = createScenarioItemList(
            uiState = stateType,
            filteredScenarios = scenarios,
            backupSelection = backupSelection,
        )

        val menuUiState = createMenuUiState(
            uiState = stateType,
            scenarioItems = scenarioList,
            backupSelection = backupSelection,
            isProModePurchased = isProMode,
        )

        ScenarioListFragmentUiState(
            type = stateType,
            menuUiState = menuUiState,
            listContent = scenarioList,
            isScenarioLimitReached = isLimitReached,
            isProModePurchased = isProMode,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    private suspend fun createScenarioItemList(
        uiState: Type,
        filteredScenarios: List<Scenario>,
        backupSelection: Set<Long>,
    ): List<ScenarioListItem> = filteredScenarios.mapNotNull { scenario ->

        if (scenario.eventCount == 0) {
            return@mapNotNull if (uiState == Type.EXPORT) null
            else ScenarioListItem.EmptyScenarioItem(scenario)
        }

        val events = repository.getEvents(scenario.id.databaseId).map { event ->
            EventItem(
                id = event.id.databaseId,
                eventName = event.name,
                actionsCount = event.actions.size,
                conditionsCount = event.conditions.size,
                firstCondition = event.conditions.first(),
            )
        }

        ScenarioListItem.ScenarioItem(
            scenario = scenario,
            eventsItems = events,
            exportMode = uiState == Type.EXPORT,
            checkedForExport = backupSelection.contains(scenario.id.databaseId),
        )
    }

    private fun createMenuUiState(
        uiState: Type,
        scenarioItems: List<ScenarioListItem>,
        backupSelection: Set<Long>,
        isProModePurchased: Boolean,
    ): Menu = when (uiState) {
        Type.SEARCH -> Menu(
            searchItemState = Menu.Item(false),
            selectAllItemState = Menu.Item(false),
            cancelItemState = Menu.Item(false),
            importItemState = Menu.Item(false),
            exportItemState = Menu.Item(false),
        )

        Type.EXPORT -> Menu(
            searchItemState = Menu.Item(false),
            selectAllItemState = Menu.Item(true),
            cancelItemState = Menu.Item(true),
            importItemState = Menu.Item(false),
            exportItemState = Menu.Item(
                visible = true,
                enabled = backupSelection.isNotEmpty(),
                iconAlpha = if (backupSelection.isNotEmpty()) ALPHA_ENABLED_ITEM_INT else ALPHA_DISABLED_ITEM_INT,
            ),
        )

        Type.SELECTION -> {
            val haveScenarioToCopy = scenarioItems.firstOrNull { it is ScenarioListItem.ScenarioItem } != null

            Menu(
                searchItemState = Menu.Item(scenarioItems.isNotEmpty()),
                selectAllItemState = Menu.Item(false),
                cancelItemState = Menu.Item(false),
                importItemState = Menu.Item(
                    visible = true,
                    enabled = true,
                    iconAlpha = if (isProModePurchased) ALPHA_ENABLED_ITEM_INT else ALPHA_DISABLED_ITEM_INT,
                ),
                exportItemState = Menu.Item(
                    visible = haveScenarioToCopy,
                    enabled = haveScenarioToCopy,
                    iconAlpha = if (isProModePurchased) ALPHA_ENABLED_ITEM_INT else ALPHA_DISABLED_ITEM_INT,
                ),
            )
        }
    }

    /** The Android notification manager. Initialized only if needed.*/
    private val notificationManager: NotificationManager?

    init {
        SmartAutoClickerService.getLocalService(serviceConnection)

        notificationManager =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                application.getSystemService(NotificationManager::class.java)
            else null
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
     * This only concerns the mandatory permissions.
     *
     * @return true if they are all granted, false if at least one is not.
     */
    fun arePermissionsGranted(): Boolean = isOverlayPermissionValid() && isAccessibilityPermissionValid()

    /** Tells if the optional notification permission is granted or not. */
    fun isNotificationPermissionGranted(): Boolean = notificationManager?.areNotificationsEnabled() ?: true

    /**
     * Create a new click scenario.
     *
     * @param context the Android context.
     * @param name the name of this new scenario.
     */
    fun createScenario(context: Context, name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addScenario(
                Scenario(
                    id = Identifier(databaseId = DATABASE_ID_INSERTION, domainId = 0L),
                    name = name,
                    detectionQuality = context.resources.getInteger(R.integer.default_detection_quality),
                    endConditionOperator = OR,
                )
            )
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
        viewModelScope.launch(Dispatchers.IO) { repository.deleteScenario(scenario.id) }
    }

    /**
     * Change the ui state type.
     * @param state the new state.
     */
    fun setUiState(state: Type) {
        uiStateType.value = state
        selectedForBackup.value = selectedForBackup.value.toMutableSet().apply { clear() }
    }

    /**
     * Toggle the selected for backup state of a scenario.
     * @param scenario the scenario to be toggled.
     */
    fun toggleScenarioSelectionForBackup(scenario: Scenario) {
        if (scenario.eventCount == 0) return

        val newSelection = selectedForBackup.value.toMutableSet().apply {
            if (contains(scenario.id.databaseId)) remove(scenario.id.databaseId)
            else add(scenario.id.databaseId)
        }
        selectedForBackup.value = newSelection
    }

    /** Toggle the selected for backup state value for all scenario. */
    fun toggleAllScenarioSelectionForBackup() {
        uiState.value?.let { state ->
            if (state.listContent.size == selectedForBackup.value.size) {
                selectedForBackup.value = emptySet()
            } else {
                selectedForBackup.value = state.listContent
                    .mapNotNull { item ->
                        if (item !is ScenarioListItem.ScenarioItem) null
                        else item.scenario.id.databaseId
                    }.toSet()
            }
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
    fun loadScenario(context: Context, resultCode: Int, data: Intent, scenario: Scenario): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val foregroundPermission = PermissionChecker.checkSelfPermission(context, Manifest.permission.FOREGROUND_SERVICE)
            if (foregroundPermission != PermissionChecker.PERMISSION_GRANTED) return false
        }

        clickerService?.start(resultCode, data, scenario)
        return true
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

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: Condition, onBitmapLoaded: (Bitmap?) -> Unit): Job? {
        if (condition.bitmap != null) {
            onBitmapLoaded.invoke(condition.bitmap)
            return null
        }

        if (condition.path != null) {
            return viewModelScope.launch(Dispatchers.IO) {
                val bitmap = repository.getBitmap(condition.path!!, condition.area.width(), condition.area.height())

                if (isActive) {
                    withContext(Dispatchers.Main) {
                        onBitmapLoaded.invoke(bitmap)
                    }
                }
            }
        }

        onBitmapLoaded.invoke(null)
        return null
    }

    fun onScenarioCountReachedAddCopyClicked(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Limitation.SCENARIO_COUNT_LIMIT)
    }

    fun onExportClickedWithoutProMode(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Feature.BACKUP_EXPORT)
    }

    fun onImportClickedWithoutProMode(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Feature.BACKUP_IMPORT)
    }
}

/**
 * Ui State for the [ScenarioListFragment]
 *
 * @param type the current ui type
 * @param menuUiState the ui state for the action bar menu
 * @param listContent the content of the scenario list
 * @param isScenarioLimitReached tells if the user don't have pro mode and have reached the scenario creation count
 * @param isProModePurchased tells if the user have bought pro mode
 */
data class ScenarioListFragmentUiState(
    val type: Type,
    val menuUiState: Menu,
    val listContent: List<ScenarioListItem>,
    val isScenarioLimitReached: Boolean,
    val isProModePurchased: Boolean,
) {

    /** Possible states for the action menu of the ScenarioListFragment. */
    enum class Type {
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
     * @param searchItemState the state of the search item.
     * @param selectAllItemState the state of the select all item.
     * @param cancelItemState the state of the cancel item.
     * @param importItemState the state of the import item.
     * @param exportItemState the state of the export item.
     */
    data class Menu(
        val searchItemState: Item,
        val selectAllItemState: Item,
        val cancelItemState: Item,
        val importItemState: Item,
        val exportItemState: Item,
    ) {

        /**
         * Defines a menu item in the action bar.
         *
         * @param visible true if it should be visible, false if not.
         * @param enabled true if the user can interact with it, false if not.
         * @param iconAlpha the alpha to apply to the icon.
         */
        data class Item(
            val visible: Boolean,
            val enabled: Boolean = true,
            @IntRange(from = 0, to = 255) val iconAlpha: Int = 255,
        )
    }

    sealed class ScenarioListItem {

        data class EmptyScenarioItem(
            val scenario: Scenario,
        ) : ScenarioListItem()

        data class ScenarioItem(
            val scenario: Scenario,
            val eventsItems: List<EventItem>,
            val exportMode: Boolean,
            val checkedForExport: Boolean,
        ) : ScenarioListItem()
    }

    data class EventItem(
        val id: Long,
        val eventName: String,
        val actionsCount: Int,
        val conditionsCount: Int,
        val firstCondition: Condition?,
    )
}
