/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import io.mockk.coEvery
import io.mockk.mockk

internal suspend fun createEditionRepository(
    scenario: Scenario,
    screenEvents: List<ScreenEvent> = emptyList(),
    triggerEvents: List<TriggerEvent> = emptyList(),
    counters: List<Counter> = emptyList(),
): EditionRepository {
    val repository = mockk<IRepository> {
        coEvery { getScenario(scenario.id.databaseId) } returns scenario
        coEvery { getScreenEvents(scenario.id.databaseId) } returns screenEvents
        coEvery { getTriggerEvents(scenario.id.databaseId) } returns triggerEvents
        coEvery { getCounters(scenario.id.databaseId) } returns counters
    }

    return EditionRepository(repository, mockk<BitmapRepository>(relaxed = true)).also {
        check(it.startEdition(scenario.id.databaseId))
    }
}
