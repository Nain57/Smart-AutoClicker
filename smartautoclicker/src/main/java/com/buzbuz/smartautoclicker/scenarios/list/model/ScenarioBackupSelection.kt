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
package com.buzbuz.smartautoclicker.scenarios.list.model

import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

data class ScenarioBackupSelection(
    val smartSelection: Set<Long> = emptySet(),
)

fun ScenarioBackupSelection.isEmpty(): Boolean =
    smartSelection.isEmpty()

/**
 * Toggle the selected for backup state of a smart scenario.
 * @param item the smart scenario to be toggled.
 */
fun ScenarioBackupSelection.toggleScenarioSelectionForBackup(item: ScenarioListUiState.Item): ScenarioBackupSelection? =
    when (item) {
        is ScenarioListUiState.Item.ScenarioItem.Valid.Smart -> toggleSmartScenarioSelectionForBackup(item.scenario)
        else -> null
    }

fun ScenarioBackupSelection.toggleAllScenarioSelectionForBackup(allItems: List<ScenarioListUiState.Item>): ScenarioBackupSelection =
    if (smartSelection.isNotEmpty()) {
        copy(smartSelection = emptySet())
    } else {
        val smartIds = mutableSetOf<Long>()

        allItems.forEach { item ->
            when (item) {
                is ScenarioListUiState.Item.ScenarioItem.Valid.Smart -> smartIds.add(item.scenario.id.databaseId)
                else -> Unit
            }
        }

        copy(smartSelection = smartIds)
    }

/**
 * Toggle the selected for backup state of a smart scenario.
 * @param scenario the smart scenario to be toggled.
 */
private fun ScenarioBackupSelection.toggleSmartScenarioSelectionForBackup(scenario: Scenario): ScenarioBackupSelection? {
    if (scenario.eventCount == 0) return null

    val newSelection = smartSelection.toMutableSet().apply {
        if (contains(scenario.id.databaseId)) remove(scenario.id.databaseId)
        else add(scenario.id.databaseId)
    }

    return copy(smartSelection = newSelection)
}

