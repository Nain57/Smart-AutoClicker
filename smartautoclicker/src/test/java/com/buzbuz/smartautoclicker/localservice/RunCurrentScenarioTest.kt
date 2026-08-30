/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.localservice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunCurrentScenarioTest {

    @Test
    fun `paused scenario with visible root overlay can run`() {
        assertTrue(canRunCurrentScenario(true, false, false, false))
    }

    @Test
    fun `missing scenario cannot run`() {
        assertFalse(canRunCurrentScenario(false, false, false, false))
    }

    @Test
    fun `running scenario is left alone`() {
        assertFalse(canRunCurrentScenario(true, true, false, false))
    }

    @Test
    fun `hidden overlay cannot run`() {
        assertFalse(canRunCurrentScenario(true, false, true, false))
    }

    @Test
    fun `settings or switcher above root prevent running`() {
        assertFalse(canRunCurrentScenario(true, false, false, true))
    }
}
