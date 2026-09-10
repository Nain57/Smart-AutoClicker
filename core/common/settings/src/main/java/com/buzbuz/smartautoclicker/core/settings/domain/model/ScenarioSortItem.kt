/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.settings.domain.model

/** The values used by the scenario lists to apply the shared sort preference. */
data class ScenarioSortItem(
    val id: Long,
    val name: String,
    val lastStartTimestamp: Long,
    val startCount: Long,
)

/**
 * Sorts any scenario-list representation using the same rules as the home screen.
 *
 * Ties are resolved by case-insensitive name and then database id so that a list does not
 * jump around when two scenarios have the same primary sort value.
 */
fun <T> Iterable<T>.sortedByScenarioSortSettings(
    settings: ScenarioSortSettings,
    item: (T) -> ScenarioSortItem,
): List<T> = sortedWith { left, right ->
    val leftItem = item(left)
    val rightItem = item(right)

    val primaryComparison = when (settings.type) {
        ScenarioSortType.NAME -> compareValues(leftItem.name, rightItem.name)
        ScenarioSortType.RECENT -> compareValues(leftItem.lastStartTimestamp, rightItem.lastStartTimestamp)
        ScenarioSortType.MOST_USED -> compareValues(leftItem.startCount, rightItem.startCount)
    }

    val orderedPrimaryComparison = when (settings.type) {
        ScenarioSortType.NAME -> if (settings.inverted) -primaryComparison else primaryComparison
        ScenarioSortType.RECENT,
        ScenarioSortType.MOST_USED,
        -> if (settings.inverted) primaryComparison else -primaryComparison
    }

    orderedPrimaryComparison
        .takeIf { it != 0 }
        ?: compareValuesBy(leftItem, rightItem, { it.name.lowercase() }, { it.id })
}
