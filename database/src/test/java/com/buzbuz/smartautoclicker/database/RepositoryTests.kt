/*
 * Copyright (C) 2021 Nain57
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

import android.content.Context
import android.graphics.Bitmap
import android.os.Build

import androidx.arch.core.executor.testing.InstantTaskExecutorRule

import com.buzbuz.smartautoclicker.database.bitmap.BitmapManager
import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.dao.ConditionDao
import com.buzbuz.smartautoclicker.database.room.dao.EventDao
import com.buzbuz.smartautoclicker.database.room.dao.ScenarioDao
import com.buzbuz.smartautoclicker.database.room.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.database.room.entity.ScenarioWithEvents
import com.buzbuz.smartautoclicker.database.utils.TestsData
import com.buzbuz.smartautoclicker.database.utils.anyNotNull

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock

import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.`when` as mockWhen

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Tests for the [RepositoryImpl]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class RepositoryTests {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    /** Coroutine dispatcher for the tests. */
    private val testDispatcher = TestCoroutineDispatcher()
    /** Coroutine scope for the tests. */
    private val testScope = TestCoroutineScope(testDispatcher)

    /** A mocked version of the bitmap manager. */
    @Mock private lateinit var mockBitmapManager: BitmapManager
    /** A mocked version of the Scenario Dao. */
    @Mock private lateinit var mockScenarioDao: ScenarioDao
    /** A mocked version of the Event Dao. */
    @Mock private lateinit var mockEventDao: EventDao
    /** A mocked version of the Condition Dao. */
    @Mock private lateinit var mockConditionDao: ConditionDao
    /** Object under tests. */
    private lateinit var repository: RepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        val mockContext = mock(Context::class.java)
        mockWhen(mockContext.filesDir).thenReturn(File(DATA_FILE_DIR))
        val mockDatabase = mock(ClickDatabase::class.java)
        mockWhen(mockDatabase.scenarioDao()).thenReturn(mockScenarioDao)
        mockWhen(mockDatabase.eventDao()).thenReturn(mockEventDao)
        mockWhen(mockDatabase.conditionDao()).thenReturn(mockConditionDao)

        repository = RepositoryImpl(mockDatabase, mockBitmapManager)
        clearInvocations(mockScenarioDao, mockEventDao, mockConditionDao)
    }

    @After
    fun tearDown() {
        testScope.cleanupTestCoroutines()
        Dispatchers.resetMain()
    }

    @Test
    fun createScenario() = runBlocking {
        repository.addScenario(TestsData.getNewScenario())
        verify(mockScenarioDao).add(TestsData.getNewScenarioEntity())
        Unit
    }

    @Test
    fun updateScenario() = runBlocking {
        repository.updateScenario(TestsData.getNewScenario())
        verify(mockScenarioDao).update(TestsData.getNewScenarioEntity())
    }

    @Test
    fun deleteScenario() = runBlocking {
        mockWhen(mockEventDao.getEventsIds(TestsData.SCENARIO_ID)).thenReturn(emptyList())
        repository.deleteScenario(TestsData.getNewScenario())

        verify(mockScenarioDao).delete(TestsData.getNewScenarioEntity())
    }

    @Test
    fun deleteScenario_removedConditions() = runBlocking {
        mockWhen(mockEventDao.getEventsIds(TestsData.SCENARIO_ID)).thenReturn(listOf(2L, 4L))
        mockWhen(mockConditionDao.getConditionsPath(2L)).thenReturn(listOf("toto", "tutu"))
        mockWhen(mockConditionDao.getConditionsPath(4L)).thenReturn(listOf("tutu"))
        mockWhen(mockConditionDao.getValidPathCount("toto")).thenReturn(1)
        mockWhen(mockConditionDao.getValidPathCount("tutu")).thenReturn(0)
        repository.deleteScenario(TestsData.getNewScenario())

        verify(mockBitmapManager).deleteBitmaps(listOf("tutu"))
    }

    @Test
    fun getScenario() = runBlocking {
        mockWhen(mockScenarioDao.getScenarioWithEvents(TestsData.SCENARIO_ID)).thenReturn(
            flow {
                emit(ScenarioWithEvents(TestsData.getNewScenarioEntity(), emptyList()))
            }
        )

        assertEquals(
            TestsData.getNewScenario(),
            repository.getScenario(TestsData.SCENARIO_ID).first(),
        )
    }

    @Test
    fun getEventList() = runBlocking {
        mockWhen(mockEventDao.getEvents(TestsData.SCENARIO_ID)).thenReturn(
            flow {
                emit(listOf(
                    TestsData.getNewEventEntity(id = 1L, scenarioId = TestsData.SCENARIO_ID, priority = 0),
                    TestsData.getNewEventEntity(id = 2L, scenarioId = TestsData.SCENARIO_ID, priority = 1),
                ))
            }
        )

        assertEquals(
            listOf(
                TestsData.getNewEvent(id = 1L, scenarioId = TestsData.SCENARIO_ID, priority = 0),
                TestsData.getNewEvent(id = 2L, scenarioId = TestsData.SCENARIO_ID, priority = 1),
            ),
            repository.getEventList(TestsData.SCENARIO_ID).first(),
        )
    }

    @Test
    fun getCompleteEventList() = runBlocking {
        mockWhen(mockEventDao.getCompleteEvents(TestsData.SCENARIO_ID)).thenReturn(
            flow {
                emit(listOf(
                    CompleteEventEntity(
                        event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0),
                        actions = listOf(TestsData.getNewPauseEntity(eventId = TestsData.EVENT_ID, priority = 0)),
                        conditions = listOf(TestsData.getNewConditionEntity(eventId = TestsData.EVENT_ID))
                    )
                ))
            }
        )

        assertEquals(
            listOf(
                TestsData.getNewEvent(
                    id = TestsData.EVENT_ID,
                    scenarioId = TestsData.SCENARIO_ID,
                    priority = 0,
                    actions = mutableListOf(TestsData.getNewPause(eventId = TestsData.EVENT_ID)),
                    conditions = mutableListOf(TestsData.getNewCondition(eventId = TestsData.EVENT_ID))
                ),
            ),
            repository.getCompleteEventList(TestsData.SCENARIO_ID).first(),
        )
    }

    @Test
    fun getCompleteEvent() = runBlocking {
        mockWhen(mockEventDao.getEvent(TestsData.EVENT_ID)).thenReturn(
            CompleteEventEntity(
                event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0),
                actions = listOf(TestsData.getNewPauseEntity(eventId = TestsData.EVENT_ID, priority = 0)),
                conditions = listOf(TestsData.getNewConditionEntity(eventId = TestsData.EVENT_ID))
            )
        )

        assertEquals(
            TestsData.getNewEvent(
                id = TestsData.EVENT_ID,
                scenarioId = TestsData.SCENARIO_ID,
                priority = 0,
                actions = mutableListOf(TestsData.getNewPause(eventId = TestsData.EVENT_ID)),
                conditions = mutableListOf(TestsData.getNewCondition(eventId = TestsData.EVENT_ID))
            ),
            repository.getCompleteEvent(TestsData.EVENT_ID),
        )
    }

    @Test
    fun getBitmap() = runBlocking {
        repository.getBitmap("toto", 20, 100)
        verify(mockBitmapManager).loadBitmap("toto", 20, 100)
        Unit
    }

    @Test
    fun addEvent_saveBitmaps() = runBlocking {
        val bitmapWithoutPath = mock(Bitmap::class.java)
        val bitmapWithPath = mock(Bitmap::class.java)
        val event = TestsData.getNewEvent(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
            actions = mutableListOf(TestsData.getNewPause(eventId = TestsData.EVENT_ID)),
            conditions = mutableListOf(
                TestsData.getNewCondition(eventId = TestsData.EVENT_ID, path = "titi", bitmap = null),
                TestsData.getNewCondition(eventId = TestsData.EVENT_ID, path = null, bitmap = bitmapWithoutPath),
                TestsData.getNewCondition(eventId = TestsData.EVENT_ID, path = "tutu", bitmap = bitmapWithPath),
            )
        )
        mockWhen(mockBitmapManager.saveBitmap(bitmapWithoutPath)).thenReturn("toto")

        repository.addEvent(event)

        verify(mockBitmapManager).saveBitmap(bitmapWithoutPath)
        verify(mockBitmapManager, never()).saveBitmap(bitmapWithPath)
        Unit
    }

    @Test
    fun addEvent_incomplete() = runBlocking {
        val event = TestsData.getNewEvent(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
        )

        repository.addEvent(event)

        verify(mockEventDao, never()).addEvent(anyNotNull())
    }

    @Test
    fun addEvent() = runBlocking {
        val event = TestsData.getNewEvent(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
            actions = mutableListOf(TestsData.getNewPause(eventId = TestsData.EVENT_ID)),
            conditions = mutableListOf(TestsData.getNewCondition(eventId = TestsData.EVENT_ID)),
        )
        val expectedEntity = CompleteEventEntity(
            event = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 0),
            actions = listOf(TestsData.getNewPauseEntity(eventId = TestsData.EVENT_ID, priority = 0)),
            conditions = listOf(TestsData.getNewConditionEntity(eventId = TestsData.EVENT_ID))
        )

        repository.addEvent(event)

        verify(mockEventDao).addEvent(expectedEntity)
    }

    @Test
    fun updateEventsPriority() = runBlocking {
        val events = listOf(
            TestsData.getNewEvent(id = 1, scenarioId = TestsData.SCENARIO_ID, priority = 1),
            TestsData.getNewEvent(id = 2, scenarioId = TestsData.SCENARIO_ID, priority = 0),

        )
        events.forEach { repository.addEvent(it) }

        val expectedEventsEntity = listOf(
            TestsData.getNewEventEntity(id = 1, scenarioId = TestsData.SCENARIO_ID, priority = 1),
            TestsData.getNewEventEntity(id = 2, scenarioId = TestsData.SCENARIO_ID, priority = 0),
        )

        repository.updateEventsPriority(events)

        verify(mockEventDao).updateEventList(expectedEventsEntity)
    }

    @Test
    fun removeEvent() = runBlocking {
        val event = TestsData.getNewEvent(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 1)
        val expectedEvent = TestsData.getNewEventEntity(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 1)
        mockWhen(mockConditionDao.getConditionsPath(TestsData.EVENT_ID)).thenReturn(emptyList())

        repository.removeEvent(event)

        verify(mockEventDao).deleteEvent(expectedEvent)
    }

    @Test
    fun removeEvent_removedConditions() = runBlocking {
        val event = TestsData.getNewEvent(id = TestsData.EVENT_ID, scenarioId = TestsData.SCENARIO_ID, priority = 1)
        val conditionsPath = listOf("tata", "toto", "tutu")
        mockWhen(mockConditionDao.getConditionsPath(TestsData.EVENT_ID)).thenReturn(conditionsPath)
        mockWhen(mockConditionDao.getValidPathCount("tata")).thenReturn(1)
        mockWhen(mockConditionDao.getValidPathCount("toto")).thenReturn(1)
        mockWhen(mockConditionDao.getValidPathCount("tutu")).thenReturn(0)

        repository.removeEvent(event)

        verify(mockBitmapManager).deleteBitmaps(listOf("tutu"))
    }

    @Test
    fun updateEvent_saveBitmaps() = runBlocking {
        val bitmapWithoutPath = mock(Bitmap::class.java)
        val bitmapWithPath = mock(Bitmap::class.java)
        val event = TestsData.getNewEvent(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
            actions = mutableListOf(TestsData.getNewPause(eventId = TestsData.EVENT_ID)),
            conditions = mutableListOf(
                TestsData.getNewCondition(eventId = TestsData.EVENT_ID, path = "titi", bitmap = null),
                TestsData.getNewCondition(eventId = TestsData.EVENT_ID, path = null, bitmap = bitmapWithoutPath),
                TestsData.getNewCondition(eventId = TestsData.EVENT_ID, path = "tutu", bitmap = bitmapWithPath),
            )
        )
        mockWhen(mockBitmapManager.saveBitmap(bitmapWithoutPath)).thenReturn("toto")
        mockWhen(mockConditionDao.getConditions(TestsData.EVENT_ID)).thenReturn(emptyList())
        mockWhen(mockEventDao.getActions(TestsData.EVENT_ID)).thenReturn(emptyList())

        repository.updateEvent(event)

        verify(mockBitmapManager).saveBitmap(bitmapWithoutPath)
        verify(mockBitmapManager, never()).saveBitmap(bitmapWithPath)
        Unit
    }

    @Test
    fun updateEvent_incomplete() = runBlocking {
        val event = TestsData.getNewEvent(
            id = TestsData.EVENT_ID,
            scenarioId = TestsData.SCENARIO_ID,
            priority = 0,
        )

        repository.updateEvent(event)

        verify(mockEventDao, never()).addEvent(anyNotNull())
    }
}

private const val DATA_FILE_DIR = "/toto/titi"