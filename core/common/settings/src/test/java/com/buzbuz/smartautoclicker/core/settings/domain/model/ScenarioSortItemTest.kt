/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.settings.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ScenarioSortItemTest {

    @Test
    fun `name sorting uses ascending and descending order`() {
        val scenarios = listOf(
            item(id = 1, name = "Bravo"),
            item(id = 2, name = "Alpha"),
        )

        assertEquals(
            listOf(2L, 1L),
            scenarios.sortedByScenarioSortSettings(settings(ScenarioSortType.NAME, inverted = false)) { it }.map { it.id },
        )
        assertEquals(
            listOf(1L, 2L),
            scenarios.sortedByScenarioSortSettings(settings(ScenarioSortType.NAME, inverted = true)) { it }.map { it.id },
        )
    }

    @Test
    fun `recent and most used sorting keep their homepage direction`() {
        val scenarios = listOf(
            item(id = 1, name = "Old", lastStartTimestamp = 10, startCount = 20),
            item(id = 2, name = "Recent", lastStartTimestamp = 20, startCount = 5),
        )

        assertEquals(
            listOf(2L, 1L),
            scenarios.sortedByScenarioSortSettings(settings(ScenarioSortType.RECENT, inverted = false)) { it }.map { it.id },
        )
        assertEquals(
            listOf(1L, 2L),
            scenarios.sortedByScenarioSortSettings(settings(ScenarioSortType.RECENT, inverted = true)) { it }.map { it.id },
        )
        assertEquals(
            listOf(1L, 2L),
            scenarios.sortedByScenarioSortSettings(settings(ScenarioSortType.MOST_USED, inverted = false)) { it }.map { it.id },
        )
        assertEquals(
            listOf(2L, 1L),
            scenarios.sortedByScenarioSortSettings(settings(ScenarioSortType.MOST_USED, inverted = true)) { it }.map { it.id },
        )
    }

    @Test
    fun `equal primary values use name and id as stable tie breakers`() {
        val scenarios = listOf(
            item(id = 2, name = "Same"),
            item(id = 1, name = "Same"),
        )

        assertEquals(
            listOf(1L, 2L),
            scenarios.sortedByScenarioSortSettings(settings(ScenarioSortType.NAME, inverted = false)) { it }.map { it.id },
        )
    }

    private fun settings(type: ScenarioSortType, inverted: Boolean) = ScenarioSortSettings(
        type = type,
        inverted = inverted,
        showSmartScenario = true,
        showDumbScenario = true,
    )

    private fun item(
        id: Long,
        name: String,
        lastStartTimestamp: Long = 0,
        startCount: Long = 0,
    ) = ScenarioSortItem(id, name, lastStartTimestamp, startCount)
}
