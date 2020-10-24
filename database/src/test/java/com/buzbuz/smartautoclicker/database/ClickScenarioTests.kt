/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.database

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.room.ScenarioWithClicks

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [ClickScenario] class. This tests the conversion between database type and api types. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ClickScenarioTests {

    @Test
    fun toEntityNoCount() {
        val clickScenario = ClickScenario(TestsData.SCENARIO_NAME, TestsData.SCENARIO_ID)
        assertEquals(TestsData.SCENARIO_ENTITY, clickScenario.toEntity())
    }

    @Test
    fun toEntityWithCount() {
        val clickScenario = ClickScenario(TestsData.SCENARIO_NAME, TestsData.SCENARIO_ID, 18)
        assertEquals(TestsData.SCENARIO_ENTITY, clickScenario.toEntity())
    }

    @Test
    fun fromEntitiesEmpty() {
        assertEquals(emptyList<ClickScenario>(), ClickScenario.fromEntities(emptyList()))
    }

    @Test
    fun fromEntitiesNoClicks() {
        val expected = listOf(
            ClickScenario(TestsData.SCENARIO_NAME, TestsData.SCENARIO_ID),
            ClickScenario(TestsData.SCENARIO_NAME_2, TestsData.SCENARIO_ID_2)
        )

        val entities = listOf(
            ScenarioWithClicks(TestsData.SCENARIO_ENTITY, emptyList()),
            ScenarioWithClicks(TestsData.SCENARIO_ENTITY_2, emptyList())
        )

        assertEquals(expected, ClickScenario.fromEntities(entities))
    }

    @Test
    fun fromEntitiesWithClicks() {
        val expected = listOf(
            ClickScenario(TestsData.SCENARIO_NAME, TestsData.SCENARIO_ID, 2),
            ClickScenario(TestsData.SCENARIO_NAME_2, TestsData.SCENARIO_ID_2, 1)
        )

        val entities = listOf(
            ScenarioWithClicks(TestsData.SCENARIO_ENTITY, listOf(TestsData.CLICK_ENTITY, TestsData.CLICK_ENTITY_2)),
            ScenarioWithClicks(TestsData.SCENARIO_ENTITY_2, listOf(TestsData.CLICK_ENTITY))
        )

        assertEquals(expected, ClickScenario.fromEntities(entities))
    }
}