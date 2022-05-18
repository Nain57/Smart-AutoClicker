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

import com.buzbuz.smartautoclicker.database.room.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.domain.utils.TestsData

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
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
    fun toCompleteEntity() {
        val completeEventEntity = TestsData.getNewEvent(
            actions = mutableListOf(
                TestsData.getNewClick(eventId = TestsData.EVENT_ID),
                TestsData.getNewSwipe(eventId = TestsData.EVENT_ID),
                TestsData.getNewPause(eventId = TestsData.EVENT_ID)
            ),
            conditions = mutableListOf(TestsData.getNewCondition(eventId = TestsData.EVENT_ID)),
            priority = 0,
            scenarioId = TestsData.SCENARIO_ID,
        ).toCompleteEntity()

        assertNotNull("Complete event is null !", completeEventEntity)
        assertEquals(
            "Event is not the same",
            TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0),
            completeEventEntity!!.event,
        )
        assertEquals(
            "Click is not the same",
            TestsData.getNewClickEntity(eventId = TestsData.EVENT_ID, priority = 0),
            completeEventEntity.actions[0]
        )
        assertEquals(
            "Swipe is not the same",
            TestsData.getNewSwipeEntity(eventId = TestsData.EVENT_ID, priority = 0),
            completeEventEntity.actions[1]
        )
        assertEquals(
            "Pause is not the same",
            TestsData.getNewPauseEntity(eventId = TestsData.EVENT_ID, priority = 0),
            completeEventEntity.actions[2]
        )
        assertEquals(
            "Condition is not the same",
            TestsData.getNewConditionEntity(eventId = TestsData.EVENT_ID),
            completeEventEntity.conditions[0]
        )
    }

    @Test
    fun toCompleteEntity_incomplete() {
        assertNull(TestsData.getNewEvent(scenarioId = TestsData.SCENARIO_ID, priority = 0).toCompleteEntity())
    }

    @Test
    fun toDomain() {
        assertEquals(
            TestsData.getNewEvent(scenarioId = TestsData.SCENARIO_ID, priority = 0),
            TestsData.getNewEventEntity(scenarioId = TestsData.SCENARIO_ID, priority = 0).toEvent()
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

    @Test
    fun cleanupIds() {
        val completeEvent = TestsData.getNewEvent(
            actions = mutableListOf(
                TestsData.getNewClick(eventId = TestsData.EVENT_ID),
                TestsData.getNewSwipe(eventId = TestsData.EVENT_ID),
                TestsData.getNewPause(eventId = TestsData.EVENT_ID)
            ),
            conditions = mutableListOf(TestsData.getNewCondition(eventId = TestsData.EVENT_ID)),
            priority = 0,
            scenarioId = TestsData.SCENARIO_ID,
        )

        completeEvent.cleanUpIds()

        assertEquals("Event id is not cleaned",0L, completeEvent.id)
        completeEvent.conditions!!.forEach { condition ->
            assertEquals("Condition id is not cleaned", 0L, condition.id)
            assertEquals("Condition event id is not cleaned", 0L, condition.eventId)
        }
        completeEvent.actions!!.forEach { action ->
            assertEquals("Action id is not cleaned", 0L, action.id)
        }
    }

    @Test
    fun deepCopy() {
        val event = TestsData.getNewEvent(
            actions = mutableListOf(
                TestsData.getNewClick(eventId = TestsData.EVENT_ID),
                TestsData.getNewSwipe(eventId = TestsData.EVENT_ID),
                TestsData.getNewPause(eventId = TestsData.EVENT_ID),
            ),
            conditions = mutableListOf(TestsData.getNewCondition(eventId = TestsData.EVENT_ID)),
            priority = 0,
            scenarioId = TestsData.SCENARIO_ID,
        )
        val copy = event.deepCopy()

        assertEquals("Event copy is not the same", event, copy)
        assertNotSame("Click is the same object", event.actions!![0], copy.actions!![0])
        assertNotSame("Swipe is the same object", event.actions!![1], copy.actions!![1])
        assertNotSame("Pause is the same object", event.actions!![2], copy.actions!![2])
        assertNotSame("Condition is the same object", event.actions!![0], copy.conditions!![0])
    }
}