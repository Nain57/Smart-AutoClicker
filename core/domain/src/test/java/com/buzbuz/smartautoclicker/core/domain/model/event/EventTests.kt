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
package com.buzbuz.smartautoclicker.core.domain.model.event

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.domain.utils.TestsData

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Event] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class EventTests {

    @Test
    fun toEntity() {
        assertEquals(
            TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0),
            TestsData.getNewEvent(scenarioId = TestsData.SCENARIO_ID, priority = 0).toEntity()
        )
    }

    @Test
    fun toDomain_fromCompleteEntity() {
        val expectedEvent = TestsData.getNewEvent(
            actions = mutableListOf(
                TestsData.getNewClick(eventId = TestsData.EVENT_ID),
                TestsData.getNewSwipe(eventId = TestsData.EVENT_ID),
                TestsData.getNewPause(eventId = TestsData.EVENT_ID)
            ),
            conditions = mutableListOf(TestsData.getNewCondition(eventId = TestsData.EVENT_ID)),
            priority = 0,
            scenarioId = TestsData.SCENARIO_ID,
        )

        val completeEvent = CompleteEventEntity(
            event = TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = mutableListOf(
                TestsData.getNewClickEntity(eventId = TestsData.EVENT_ID, priority = 0),
                TestsData.getNewSwipeEntity(eventId = TestsData.EVENT_ID, priority = 0),
                TestsData.getNewPauseEntity(eventId = TestsData.EVENT_ID, priority = 0)
            ),
            conditions = mutableListOf(TestsData.getNewConditionEntity(eventId = TestsData.EVENT_ID)),
        ).toEvent()

        assertEquals("Complete event is not as expected", expectedEvent, completeEvent)
    }
}