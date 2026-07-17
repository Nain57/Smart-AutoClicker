/*
 * Copyright (C) 2026 Kevin Buzeau
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.creation

import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.ui.createEditionRepository

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class CounterCreationViewModelTests {

    @Test
    fun createCounter_ignoresBlankName() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val existingCounter = Counter("existing", 1.0, scenario.id)
        val editionRepository = createEditionRepository(scenario, counters = listOf(existingCounter))
        val viewModel = CountersCreationViewModel(editionRepository)

        viewModel.setName("   ")
        viewModel.createCounter()

        assertEquals(listOf(existingCounter), editionRepository.editionState.getAllEditedCounters())
    }

    @Test
    fun createCounter_ignoresDuplicateName() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val existingCounter = Counter("existing", 1.0, scenario.id)
        val editionRepository = createEditionRepository(scenario, counters = listOf(existingCounter))
        val viewModel = CountersCreationViewModel(editionRepository)

        viewModel.setName(existingCounter.counterName)
        viewModel.createCounter()

        assertEquals(listOf(existingCounter), editionRepository.editionState.getAllEditedCounters())
    }

    @Test
    fun createCounter_usesEditedScenarioAndSelectedStartingValue() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val editionRepository = createEditionRepository(scenario)
        val viewModel = CountersCreationViewModel(editionRepository)

        viewModel.setName("score")
        viewModel.setStartingValue(42.5)
        viewModel.createCounter()

        assertEquals(
            listOf(Counter(counterName = "score", defaultValue = 42.5, scenarioId = scenario.id)),
            editionRepository.editionState.getAllEditedCounters(),
        )
    }
}
