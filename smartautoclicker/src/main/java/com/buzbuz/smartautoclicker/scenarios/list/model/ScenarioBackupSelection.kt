
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

