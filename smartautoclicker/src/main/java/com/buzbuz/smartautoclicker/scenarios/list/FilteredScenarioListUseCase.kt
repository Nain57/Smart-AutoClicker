
package com.buzbuz.smartautoclicker.scenarios.list

import android.content.Context
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState
import com.buzbuz.smartautoclicker.scenarios.list.sort.ScenarioSortConfig
import com.buzbuz.smartautoclicker.scenarios.list.sort.ScenarioSortConfigRepository
import com.buzbuz.smartautoclicker.scenarios.list.sort.ScenarioSortType

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class FilteredScenarioListUseCase @Inject constructor(
    @ApplicationContext context: Context,
    sortConfigRepository: ScenarioSortConfigRepository,
    settingsRepository: SettingsRepository,
    private val smartRepository: IRepository,
) {

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    private val refresh: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)

    /** Smart scenarios only; still reacts to `refresh` emissions. */
    private val allScenarios: Flow<List<ScenarioListUiState.Item.ScenarioItem>> =
        combine(refresh, smartRepository.scenarios) { _, smartList ->
            smartList.map { it.toItem() }
        }

    /** Flow upon the list of Smart scenarios, filtered with the search query and ordered with the sort config */
    val orderedItems: Flow<List<ScenarioListUiState.Item>> =
        combine(
            allScenarios,
            searchQuery,
            sortConfigRepository.getSortConfig(),
            settingsRepository.isFilterScenarioUiEnabledFlow,
        ) { scenarios, searchQuery, sortConfig, filtersEnabled ->
            if (searchQuery == null) {
                if (filtersEnabled) {
                    val filteredAndSortedItems = scenarios.sortAndFilter(sortConfig)
                    val sortItem = ScenarioListUiState.Item.SortItem(
                        sortType = sortConfig.type,
                        smartVisible = sortConfig.showSmartScenario,
                        changeOrderChecked = sortConfig.inverted,
                    )

                    buildList {
                        if (scenarios.isNotEmpty()) add(sortItem)
                        addAll(filteredAndSortedItems)
                    }
                } else {
                    scenarios
                }
            } else {
                scenarios.filterByName(searchQuery)
            }
        }

    init {
        refresh.tryEmit(Unit)
    }

    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
    }

    suspend fun refresh() {
        refresh.emit(Unit)
    }

    private suspend fun Scenario.toItem(): ScenarioListUiState.Item.ScenarioItem =
        if (eventCount == 0) ScenarioListUiState.Item.ScenarioItem.Empty.Smart(
            scenario = this,
            lastStartTimestamp = stats?.lastStartTimestampMs ?: 0,
            startCount = stats?.startCount ?: 0
        )
        else ScenarioListUiState.Item.ScenarioItem.Valid.Smart(
            scenario = this,
            eventsItems = smartRepository.getImageEvents(id.databaseId).map { event ->
                ScenarioListUiState.Item.ScenarioItem.Valid.Smart.EventItem(
                    id = event.id.databaseId,
                    eventName = event.name,
                    actionsCount = event.actions.size,
                    conditionsCount = event.conditions.size,
                    firstCondition = if (event.conditions.isNotEmpty()) event.conditions.first() else null,
                )
            },
            triggerEventCount = smartRepository.getTriggerEvents(id.databaseId).size,
            detectionQuality = detectionQuality,
            lastStartTimestamp = stats?.lastStartTimestampMs ?: 0,
            startCount = stats?.startCount ?: 0
        )
}

private fun List<ScenarioListUiState.Item.ScenarioItem>.filterByName(
    filter: String
): List<ScenarioListUiState.Item.ScenarioItem> =
    mapNotNull { scenario ->
        if (scenario.displayName.contains(filter, true)) scenario else null
    }

private fun Collection<ScenarioListUiState.Item.ScenarioItem>.sortAndFilter(
    sortConfig: ScenarioSortConfig,
): Collection<ScenarioListUiState.Item.ScenarioItem> {

    val filteredList = filter { item ->
        (sortConfig.showSmartScenario && item.scenario is Scenario)
    }

    return when (sortConfig.type) {
        ScenarioSortType.NAME ->
            if (sortConfig.inverted) filteredList.sortedByDescending { it.displayName }
            else filteredList.sortedBy { it.displayName }

        ScenarioSortType.RECENT ->
            if (sortConfig.inverted) filteredList.sortedBy { it.lastStartTimestamp }
            else filteredList.sortedByDescending { it.lastStartTimestamp }

        ScenarioSortType.MOST_USED ->
            if (sortConfig.inverted) filteredList.sortedBy { it.startCount }
            else filteredList.sortedByDescending { it.startCount }
    }
}
