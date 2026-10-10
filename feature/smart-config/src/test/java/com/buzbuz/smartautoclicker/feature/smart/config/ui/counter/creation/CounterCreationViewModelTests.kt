/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.counter.creation

import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import io.mockk.coEvery
import io.mockk.mockk

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

    private suspend fun createEditionRepository(
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

        return EditionRepository(
            repository,
            mockk<BitmapRepository>(relaxed = true),
            mockk<DisplayConfigManager>(relaxed = true),
        ).also {
            check(it.startEdition(scenario.id.databaseId))
        }
    }
}