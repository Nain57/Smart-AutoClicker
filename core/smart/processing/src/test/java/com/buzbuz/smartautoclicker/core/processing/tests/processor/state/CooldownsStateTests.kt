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
package com.buzbuz.smartautoclicker.core.processing.tests.processor.state

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.processing.data.processor.state.CooldownsState

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CooldownsStateTests {

    private lateinit var cooldownsState: CooldownsState

    @Before
    fun setUp() {
        cooldownsState = CooldownsState()
    }

    private fun newScreenEvent(id: Long, cooldownMs: Long = 0L) = ScreenEvent(
        id = Identifier(databaseId = id),
        scenarioId = Identifier(databaseId = 1L),
        name = "TestEvent",
        conditionOperator = AND,
        actions = emptyList(),
        conditions = emptyList(),
        enabledOnStart = true,
        priority = 0,
        keepDetecting = false,
        cooldownMs = cooldownMs,
    )

    // ---- startCooldownIfNeeded ----

    @Test
    fun startCooldownIfNeeded_zeroCooldown_cooldownNotStarted() {
        val event = newScreenEvent(id = 1L, cooldownMs = 0L)

        cooldownsState.startCooldownIfNeeded(event)

        assertFalse(cooldownsState.isCooldownRunning(event))
    }

    @Test
    fun startCooldownIfNeeded_negativeCooldown_cooldownNotStarted() {
        val event = newScreenEvent(id = 1L, cooldownMs = -1L)

        cooldownsState.startCooldownIfNeeded(event)

        assertFalse(cooldownsState.isCooldownRunning(event))
    }

    @Test
    fun startCooldownIfNeeded_positiveCooldown_cooldownIsRunning() {
        val event = newScreenEvent(id = 1L, cooldownMs = 10_000L)

        cooldownsState.startCooldownIfNeeded(event)

        assertTrue(cooldownsState.isCooldownRunning(event))
    }

    @Test
    fun startCooldownIfNeeded_calledTwice_cooldownRestarts() {
        val event = newScreenEvent(id = 1L, cooldownMs = 1L)

        cooldownsState.startCooldownIfNeeded(event)
        Thread.sleep(10L) // let first cooldown expire

        // Restart before checking — should be running again
        val eventWithLongCooldown = newScreenEvent(id = 1L, cooldownMs = 10_000L)
        cooldownsState.startCooldownIfNeeded(eventWithLongCooldown)

        assertTrue(cooldownsState.isCooldownRunning(eventWithLongCooldown))
    }

    // ---- isCooldownRunning ----

    @Test
    fun isCooldownRunning_noCooldownStarted_false() {
        val event = newScreenEvent(id = 1L)

        assertFalse(cooldownsState.isCooldownRunning(event))
    }

    @Test
    fun isCooldownRunning_cooldownRunning_true() {
        val event = newScreenEvent(id = 1L, cooldownMs = 10_000L)
        cooldownsState.startCooldownIfNeeded(event)

        assertTrue(cooldownsState.isCooldownRunning(event))
    }

    @Test
    fun isCooldownRunning_cooldownExpired_false() {
        val event = newScreenEvent(id = 1L, cooldownMs = 1L)
        cooldownsState.startCooldownIfNeeded(event)

        Thread.sleep(10L)

        assertFalse(cooldownsState.isCooldownRunning(event))
    }

    @Test
    fun isCooldownRunning_cooldownExpired_subsequentCallAlsoFalse() {
        val event = newScreenEvent(id = 1L, cooldownMs = 1L)
        cooldownsState.startCooldownIfNeeded(event)
        Thread.sleep(10L)
        cooldownsState.isCooldownRunning(event) // triggers internal cleanup

        assertFalse(cooldownsState.isCooldownRunning(event))
    }

    @Test
    fun isCooldownRunning_twoEvents_checkedIndependently() {
        val eventRunning = newScreenEvent(id = 1L, cooldownMs = 10_000L)
        val eventNotRunning = newScreenEvent(id = 2L, cooldownMs = 0L)
        cooldownsState.startCooldownIfNeeded(eventRunning)
        cooldownsState.startCooldownIfNeeded(eventNotRunning)

        assertTrue(cooldownsState.isCooldownRunning(eventRunning))
        assertFalse(cooldownsState.isCooldownRunning(eventNotRunning))
    }

    // ---- removeCooldown ----

    @Test
    fun removeCooldown_runningCooldown_cooldownStopped() {
        val event = newScreenEvent(id = 1L, cooldownMs = 10_000L)
        cooldownsState.startCooldownIfNeeded(event)

        cooldownsState.removeCooldown(event)

        assertFalse(cooldownsState.isCooldownRunning(event))
    }

    @Test
    fun removeCooldown_noCooldownStarted_noError() {
        val event = newScreenEvent(id = 1L)

        cooldownsState.removeCooldown(event)

        assertFalse(cooldownsState.isCooldownRunning(event))
    }

    @Test
    fun removeCooldown_onlyRemovesTargetEvent() {
        val event1 = newScreenEvent(id = 1L, cooldownMs = 10_000L)
        val event2 = newScreenEvent(id = 2L, cooldownMs = 10_000L)
        cooldownsState.startCooldownIfNeeded(event1)
        cooldownsState.startCooldownIfNeeded(event2)

        cooldownsState.removeCooldown(event1)

        assertFalse(cooldownsState.isCooldownRunning(event1))
        assertTrue(cooldownsState.isCooldownRunning(event2))
    }
}
