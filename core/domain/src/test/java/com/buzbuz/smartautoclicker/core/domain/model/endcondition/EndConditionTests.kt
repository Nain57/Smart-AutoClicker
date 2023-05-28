/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model.endcondition

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.domain.utils.TestsData

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [EndCondition] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EndConditionTests {

    @Test
    fun toEntity() {
        Assert.assertEquals(
            TestsData.getNewEndConditionEntity(),
            TestsData.getNewEndCondition().toEntity()
        )
    }

    @Test(expected = IllegalStateException::class)
    fun toEntityWithNoScenario() {
        TestsData.getNewEndCondition(scenarioId = 0L).toEntity()
    }

    @Test(expected = IllegalStateException::class)
    fun toEntityWithNoEvents() {
        TestsData.getNewEndCondition(eventId = 0L).toEntity()
    }

    @Test
    fun toEndCondition() {
        val defaultEvent = TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0)

        Assert.assertEquals(
            TestsData.getNewEndCondition(),
            TestsData.getNewEndConditionWithEvent(event = defaultEvent).toEndCondition()
        )
    }
}