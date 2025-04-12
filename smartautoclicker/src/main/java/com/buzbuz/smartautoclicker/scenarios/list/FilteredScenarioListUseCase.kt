/*
 * Copyright (C) 2025 Kevin Buzeau
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

import android.content.Context
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.Repeatable
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.scenarios.list.model.ScenarioListUiState
import com.buzbuz.smartautoclicker.scenarios.list.sort.ScenarioSortConfig
import com.buzbuz.smartautoclicker.scenarios.list.sort.ScenarioSortConfigRepository
import com.buzbuz.smartautoclicker.scenarios.list.sort.ScenarioSortType

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class FilteredScenarioListUseCase @Inject constructor(
    @ApplicationContext context: Context,
    dumbRepository: IDumbRepository,
    sortConfigRepository: ScenarioSortConfigRepository,
    settingsRepository: SettingsRepository,
    private val smartRepository: IRepository,
) {

    /** The currently searched action name. Null if no is. */
    private val searchQuery = MutableStateFlow<String?>(null)

    /** Dumb & Smart scenario together. */
    private val allScenarios: Flow<List<ScenarioListUiState.Item.ScenarioItem>> =
        combine(dumbRepository.dumbScenarios, smartRepository.scenarios) { dumbList, smartList ->
            mutableListOf<ScenarioListUiState.Item.ScenarioItem>().apply {
                addAll(dumbList.map { it.toItem(context) })
                addAll(smartList.map { it.toItem() })
            }
        }

    /** Flow upon the list of Dumb & Smart scenarios, filtered with the search query and ordered with the sort config */
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
                        dumbVisible = sortConfig.showDumbScenario,
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

    fun updateSearchQuery(query: String?) {
        searchQuery.value = query
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
                    firstCondition = if (event.conditions.isNotEmpty()) {
                        //TODO: handle text condition
                        val condition = event.conditions.first()
                        if (condition is ImageCondition) condition
                        else null
                    } else null,
                )
            },
            triggerEventCount = smartRepository.getTriggerEvents(id.databaseId).size,
            detectionQuality = detectionQuality,
            lastStartTimestamp = stats?.lastStartTimestampMs ?: 0,
            startCount = stats?.startCount ?: 0
        )

    private fun DumbScenario.toItem(context: Context): ScenarioListUiState.Item.ScenarioItem =
        if (dumbActions.isEmpty()) ScenarioListUiState.Item.ScenarioItem.Empty.Dumb(
            scenario = this,
            lastStartTimestamp = stats?.lastStartTimestampMs ?: 0,
            startCount = stats?.startCount ?: 0
        )
        else ScenarioListUiState.Item.ScenarioItem.Valid.Dumb(
            scenario = this,
            clickCount = dumbActions.count { it is DumbAction.DumbClick },
            swipeCount = dumbActions.count { it is DumbAction.DumbSwipe },
            pauseCount = dumbActions.count { it is DumbAction.DumbPause },
            repeatText = getRepeatDisplayText(context),
            maxDurationText = getMaxDurationDisplayText(context),
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

private fun Repeatable.getRepeatDisplayText(context: Context): String =
    if (isRepeatInfinite) context.getString(R.string.item_desc_dumb_scenario_repeat_infinite)
    else context.getString(R.string.item_desc_dumb_scenario_repeat_count, repeatCount)

private fun DumbScenario.getMaxDurationDisplayText(context: Context): String =
    if (isDurationInfinite) context.getString(R.string.item_desc_dumb_scenario_max_duration_infinite)
    else context.getString(
        R.string.item_desc_dumb_scenario_max_duration,
        formatDuration(maxDurationMin.minutes.inWholeMilliseconds),
    )

private fun Collection<ScenarioListUiState.Item.ScenarioItem>.sortAndFilter(
    sortConfig: ScenarioSortConfig,
): Collection<ScenarioListUiState.Item.ScenarioItem> {

    val filteredList = filter { item ->
        (sortConfig.showSmartScenario && item.scenario is Scenario) ||
                (sortConfig.showDumbScenario && item.scenario is DumbScenario)
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
