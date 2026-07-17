/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event

import android.content.Context
import android.os.Build

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.createEditionRepository

import io.mockk.coEvery
import io.mockk.mockk

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression tests for opening the shared event dialog while a trigger event is edited. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EventDialogViewModelTests {

    @Test
    fun triggerEvent_doesNotPretendToBeScreenEvent_orExposeTryInfo() = runTest {
        val scenario = Scenario(
            id = Identifier(databaseId = 1L),
            name = "Scenario",
            detectionQuality = 600,
        )
        val triggerEvent = TriggerEvent(
            id = Identifier(databaseId = 2L),
            scenarioId = scenario.id,
            name = "Trigger event",
            conditionOperator = AND,
        )
        val repository = mockk<IRepository> {
            coEvery { getScenario(1L) } returns scenario
            coEvery { getScreenEvents(1L) } returns emptyList()
            coEvery { getTriggerEvents(1L) } returns emptyList()
            coEvery { getCounters(1L) } returns emptyList()
        }
        val editionRepository = EditionRepository(repository, mockk(relaxed = true))
        editionRepository.startEdition(1L)
        editionRepository.startEventEdition(triggerEvent)

        val viewModel = EventDialogViewModel(
            context = mockk<Context>(),
            bitmapRepository = mockk<BitmapRepository>(),
            editionRepository = editionRepository,
            monitoredViewsManager = mockk<MonitoredViewsManager>(),
            settingsRepository = mockk<SettingsRepository>(),
            tutorialRepository = mockk<TutorialRepository>(),
        )

        assertFalse(viewModel.isConfiguringScreenEvent())
        assertNull(viewModel.getTryInfo())
    }

    @Test
    fun triggerEvent_ignoresScreenOnlyControls() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val triggerEvent = TriggerEvent(Identifier(databaseId = 2L), scenario.id, "Trigger event", AND)
        val editionRepository = createEditionRepository(scenario)
        editionRepository.startEventEdition(triggerEvent)
        val viewModel = EventDialogViewModel(
            context = mockk<Context>(),
            bitmapRepository = mockk<BitmapRepository>(),
            editionRepository = editionRepository,
            monitoredViewsManager = mockk<MonitoredViewsManager>(),
            settingsRepository = mockk<SettingsRepository>(),
            tutorialRepository = mockk<TutorialRepository>(),
        )

        viewModel.toggleKeepDetectingState()
        viewModel.toggleCooldownState()
        viewModel.setCooldownValue(500)

        assertEquals(triggerEvent, editionRepository.editionState.getEditedEvent())
    }
}
