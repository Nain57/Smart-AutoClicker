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
package com.buzbuz.smartautoclicker.activity.list

import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

data class ScenarioBackupSelection(
    val dumbSelection: Set<Long> = emptySet(),
    val smartSelection: Set<Long> = emptySet(),
)

fun ScenarioBackupSelection.isEmpty(): Boolean =
    dumbSelection.isEmpty() && smartSelection.isEmpty()

/**
 * Toggle the selected for backup state of a smart scenario.
 * @param item the smart scenario to be toggled.
 */
fun ScenarioBackupSelection.toggleScenarioSelectionForBackup(item: ScenarioListUiState.Item): ScenarioBackupSelection? =
    when (item) {
        is ScenarioListUiState.Item.Valid.Dumb -> toggleDumbScenarioSelectionForBackup(item.scenario)
        is ScenarioListUiState.Item.Valid.Smart -> toggleSmartScenarioSelectionForBackup(item.scenario)
        else -> null
    }

fun ScenarioBackupSelection.toggleAllScenarioSelectionForBackup(allItems: List<ScenarioListUiState.Item>): ScenarioBackupSelection =
    if (dumbSelection.isNotEmpty() || smartSelection.isNotEmpty()) {
        copy(dumbSelection = emptySet(), smartSelection = emptySet())
    } else {
        val dumbIds = mutableSetOf<Long>()
        val smartIds = mutableSetOf<Long>()

        allItems.forEach { item ->
            when (item) {
                is ScenarioListUiState.Item.Valid.Dumb -> dumbIds.add(item.scenario.id.databaseId)
                is ScenarioListUiState.Item.Valid.Smart -> smartIds.add(item.scenario.id.databaseId)
                else -> Unit
            }
        }

        copy(dumbSelection = dumbIds, smartSelection = smartIds)
    }

/**
 * Toggle the selected for backup state of a dumb scenario.
 * @param scenario the dumb scenario to be toggled.
 */
private fun ScenarioBackupSelection.toggleDumbScenarioSelectionForBackup(scenario: DumbScenario): ScenarioBackupSelection? {
    if (scenario.dumbActions.isEmpty()) return null

    val newSelection = dumbSelection.toMutableSet().apply {
        if (contains(scenario.id.databaseId)) remove(scenario.id.databaseId)
        else add(scenario.id.databaseId)
    }

    return copy(dumbSelection = newSelection)
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

