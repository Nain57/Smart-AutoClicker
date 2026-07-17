/*
 * Copyright (C) 2026 Kevin Buzeau
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario

import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.domain.model.counter.Counter
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.feature.smart.config.ui.createEditionRepository

import io.mockk.mockk

import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioDialogViewModelTests {

    @Test
    fun counterOnlyChange_marksScenarioAsHavingUnsavedModifications() = runTest {
        val scenario = Scenario(Identifier(databaseId = 1L), "Scenario", detectionQuality = 600)
        val editionRepository = createEditionRepository(scenario)
        val viewModel = ScenarioDialogViewModel(editionRepository, mockk<MonitoredViewsManager>())
        shadowOf(android.os.Looper.getMainLooper()).idle()

        editionRepository.addNewCounter(Counter("count", 2.0, scenario.id))
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertTrue(viewModel.hasUnsavedModifications())
    }
}
