/*
 * Copyright (C) 2026 Kevin Buzeau
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.triggerevents

import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.availability.IsTriggerEventCopyAvailableUseCase
import com.buzbuz.smartautoclicker.feature.smart.config.ui.createEditionRepository

import io.mockk.mockk

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class TriggerEventListViewModelTests {

    @Test
    fun createNewEvent_belongsToTheEditedScenario() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val editionRepository = createEditionRepository(scenario)
        val viewModel = TriggerEventListViewModel(
            mockk<IsTriggerEventCopyAvailableUseCase>(relaxed = true),
            editionRepository,
        )

        val event = viewModel.createNewEvent(RuntimeEnvironment.getApplication())

        assertEquals(scenario.id, event.scenarioId)
    }

    @Test
    fun editSaveDeleteAndDismiss_updateTheEditedTriggerEvent() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val triggerEvent = TriggerEvent(Identifier(databaseId = 2L), scenario.id, "Trigger event", AND)
        val editionRepository = createEditionRepository(scenario, triggerEvents = listOf(triggerEvent))
        val viewModel = TriggerEventListViewModel(
            mockk<IsTriggerEventCopyAvailableUseCase>(relaxed = true),
            editionRepository,
        )

        viewModel.startEventEdition(triggerEvent)
        editionRepository.updateEditedEvent(triggerEvent.copy(name = "Renamed trigger"))
        viewModel.saveEventEdition()
        assertEquals("Renamed trigger", editionRepository.editionState.getAllEditedEvents().single().name)

        viewModel.startEventEdition(editionRepository.editionState.getAllEditedEvents().single() as TriggerEvent)
        viewModel.dismissEditedEvent()
        assertNull(editionRepository.editionState.getEditedEvent())

        viewModel.startEventEdition(editionRepository.editionState.getAllEditedEvents().single() as TriggerEvent)
        viewModel.deleteEditedEvent()
        assertEquals(emptyList<TriggerEvent>(), editionRepository.editionState.getAllEditedEvents())
    }
}
