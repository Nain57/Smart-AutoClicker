/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.domain

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.room.entity.ScenarioWithEvents
import com.buzbuz.smartautoclicker.domain.utils.TestsData

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Scenario] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioTests {

    @Test
    fun toEntityNoCount() {
        assertEquals(
            TestsData.getNewScenarioEntity(),
            TestsData.getNewScenario().toEntity()
        )
    }

    @Test
    fun toEntityWithCount() {
        assertEquals(
            TestsData.getNewScenarioEntity(),
            TestsData.getNewScenario(eventCount = 18).toEntity()
        )
    }

    @Test
    fun toScenarioWithEvents() {
        val scenario = ScenarioWithEvents(
            TestsData.getNewScenarioEntity(),
            listOf(TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0))
        ).toScenario()
        val expectedScenario = TestsData.getNewScenario(TestsData.SCENARIO_ID, TestsData.SCENARIO_NAME, eventCount = 1)

        assertEquals(scenario, expectedScenario)
    }

    @Test
    fun toScenarioComplete() {
        val scenario = ScenarioWithEvents(
            TestsData.getNewScenarioEntity(),
            listOf(TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0))
        ).toScenario()
        val expectedScenario = TestsData.getNewScenario(
            TestsData.SCENARIO_ID,
            TestsData.SCENARIO_NAME,
            TestsData.SCENARIO_DETECTION_QUALITY,
            TestsData.SCENARIO_END_CONDITION_OPERATOR,
            eventCount = 1
        )

        assertEquals(scenario, expectedScenario)
    }
}