/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.scenariostate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioStateTest {

    @Test
    fun `public contract exposes only scenario name and state`() {
        assertEquals(2, ScenarioStatePluginContract.relevantVariables.size)
        assertTrue(ScenarioStatePluginContract.relevantVariables[0].startsWith("%klickr_scenario_name\n"))
        assertTrue(ScenarioStatePluginContract.relevantVariables[1].startsWith("%klickr_scenario_state\n"))
    }

    @Test
    fun `no loaded scenario has none state`() {
        assertEquals(
            ScenarioState.NONE,
            ScenarioState.from(
                isScenarioOpen = false,
                isRunning = true,
                isOverlayHidden = true,
                isSettingsOpen = true,
            ),
        )
    }

    @Test
    fun `settings takes priority over every loaded scenario state`() {
        assertEquals(
            ScenarioState.SETTINGS,
            ScenarioState.from(
                isScenarioOpen = true,
                isRunning = true,
                isOverlayHidden = true,
                isSettingsOpen = true,
            ),
        )
    }

    @Test
    fun `hidden takes priority over running`() {
        assertEquals(
            ScenarioState.HIDDEN,
            ScenarioState.from(
                isScenarioOpen = true,
                isRunning = true,
                isOverlayHidden = true,
                isSettingsOpen = false,
            ),
        )
    }

    @Test
    fun `visible loaded scenario reports running or paused`() {
        assertEquals(ScenarioState.RUNNING, visibleScenarioState(isRunning = true))
        assertEquals(ScenarioState.PAUSED, visibleScenarioState(isRunning = false))
    }

    @Test
    fun `condition is satisfied for every loaded state`() {
        ScenarioState.entries
            .filterNot { it == ScenarioState.NONE }
            .forEach { assertTrue(ScenarioStateSnapshot("Scenario", it).isScenarioOpen) }

        assertFalse(ScenarioStateSnapshot("", ScenarioState.NONE).isScenarioOpen)
    }

    @Test
    fun `state values form the documented stable vocabulary`() {
        assertEquals(
            listOf("running", "paused", "hidden", "settings", "none"),
            ScenarioState.entries.map { it.value },
        )
    }

    private fun visibleScenarioState(isRunning: Boolean): ScenarioState =
        ScenarioState.from(
            isScenarioOpen = true,
            isRunning = isRunning,
            isOverlayHidden = false,
            isSettingsOpen = false,
        )
}
