/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain

import android.content.Intent
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.externallaunch.domain.ExternalLaunchRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalePluginActionExecutorTest {

    private val smartRepository = mockk<IRepository>()
    private val dumbRepository = mockk<DumbRepository>()
    private val externalLaunchRepository = mockk<ExternalLaunchRepository>(relaxed = true)
    private val executor = LocalePluginActionExecutor(
        ioDispatcher = UnconfinedTestDispatcher(),
        smartRepository = smartRepository,
        dumbRepository = dumbRepository,
        externalLaunchRepository = externalLaunchRepository,
    )

    @Test
    fun `stop resolves without a scenario and executes stop`() = runTest {
        val action = executor.resolve(LocalePluginConfiguration(operation = LocalePluginOperation.STOP))

        assertSame(ResolvedLocalePluginAction.Stop, action)
        executor.executeStop()
        verify(exactly = 1) { externalLaunchRepository.stopScenarios() }
    }

    @Test
    fun `smart launch resolves and uses atomic replacement with fresh projection`() = runTest {
        val scenario = mockk<Scenario>()
        coEvery { smartRepository.getScenario(42L) } returns scenario

        val action = executor.resolve(
            LocalePluginConfiguration(
                operation = LocalePluginOperation.LAUNCH,
                scenarioId = 42L,
                isSmart = true,
            )
        )

        assertTrue(action is ResolvedLocalePluginAction.LaunchSmart)
        assertSame(scenario, (action as ResolvedLocalePluginAction.LaunchSmart).scenario)
        val projectionData = mockk<Intent>()
        executor.launchSmart(RESULT_OK, projectionData, action)
        verify(exactly = 1) {
            externalLaunchRepository.replaceSmartScenario(RESULT_OK, projectionData, scenario)
        }
    }

    @Test
    fun `smart launch reuses active projection`() = runTest {
        val scenario = scenario(42L)
        every { externalLaunchRepository.isSmartScreenRecordActive() } returns true
        every { externalLaunchRepository.getSmartScenarioId() } returns 7L

        val action = ResolvedLocalePluginAction.LaunchSmart(scenario)

        assertTrue(executor.launchSmartWithCurrentProjection(action))
        verify(exactly = 1) { externalLaunchRepository.replaceSmartScenarioWithCurrentProjection(scenario) }
    }

    @Test
    fun `smart launch leaves the already active scenario untouched`() = runTest {
        val scenario = scenario(42L)
        every { externalLaunchRepository.isSmartScreenRecordActive() } returns true
        every { externalLaunchRepository.getSmartScenarioId() } returns 42L
        every { externalLaunchRepository.isAccessibilityServiceStarted() } returns true

        assertTrue(executor.launchSmartWithCurrentProjection(ResolvedLocalePluginAction.LaunchSmart(scenario)))
        verify(exactly = 0) { externalLaunchRepository.replaceSmartScenarioWithCurrentProjection(any()) }
    }

    @Test
    fun `smart launch does not reuse missing projection`() = runTest {
        val scenario = scenario(42L)
        every { externalLaunchRepository.isSmartScreenRecordActive() } returns false

        val action = ResolvedLocalePluginAction.LaunchSmart(scenario)

        assertNull(executor.launchSmartWithCurrentProjection(action).takeIf { it })
        verify(exactly = 0) { externalLaunchRepository.replaceSmartScenarioWithCurrentProjection(any()) }
    }

    @Test
    fun `reports when the user is editing the current scenario`() {
        every { externalLaunchRepository.isScenarioConfigurationOpen() } returns true

        assertTrue(executor.isScenarioConfigurationOpen())
    }

    @Test
    fun `dumb launch resolves and uses atomic replacement`() = runTest {
        val scenario = mockk<DumbScenario>()
        every { scenario.id } returns Identifier(databaseId = 7L)
        coEvery { dumbRepository.getDumbScenario(7L) } returns scenario

        val action = executor.resolve(
            LocalePluginConfiguration(
                operation = LocalePluginOperation.LAUNCH,
                scenarioId = 7L,
                isSmart = false,
            )
        )

        assertTrue(action is ResolvedLocalePluginAction.LaunchDumb)
        executor.launchDumb(action as ResolvedLocalePluginAction.LaunchDumb)
        verify(exactly = 1) { externalLaunchRepository.replaceDumbScenario(scenario) }
    }

    @Test
    fun `dumb launch leaves the already running scenario untouched`() {
        val scenario = mockk<DumbScenario>()
        every { scenario.id } returns Identifier(databaseId = 7L)
        every { externalLaunchRepository.isDumbScenarioRunning(7L) } returns true

        executor.launchDumb(ResolvedLocalePluginAction.LaunchDumb(scenario))

        verify(exactly = 0) { externalLaunchRepository.replaceDumbScenario(any()) }
    }

    @Test
    fun `deleted scenarios do not resolve`() = runTest {
        coEvery { smartRepository.getScenario(99L) } returns null

        assertNull(
            executor.resolve(
                LocalePluginConfiguration(
                    operation = LocalePluginOperation.LAUNCH,
                    scenarioId = 99L,
                    isSmart = true,
                )
            )
        )
    }
}

private const val RESULT_OK = -1

private fun scenario(id: Long) = Scenario(
    id = Identifier(databaseId = id),
    name = "Scenario $id",
    detectionQuality = 100,
)
