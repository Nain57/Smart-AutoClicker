/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.domain

import android.content.Context
import android.os.Build

import com.buzbuz.smartautoclicker.backup.BackupEngine
import com.buzbuz.smartautoclicker.database.bitmap.BitmapManager
import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.dao.ConditionDao
import com.buzbuz.smartautoclicker.database.room.dao.EndConditionDao
import com.buzbuz.smartautoclicker.database.room.dao.EventDao
import com.buzbuz.smartautoclicker.database.room.dao.ScenarioDao
import com.buzbuz.smartautoclicker.database.room.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.database.room.entity.ScenarioWithEndConditions
import com.buzbuz.smartautoclicker.domain.utils.TestsData

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*

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

    /** A mocked version of the bitmap manager. */
    @Mock private lateinit var mockBitmapManager: BitmapManager
    /** A mocked version of the backup engine. */
    @Mock private lateinit var mockBackupEngine: BackupEngine
    /** A mocked version of the Scenario Dao. */
    @Mock private lateinit var mockScenarioDao: ScenarioDao
    /** A mocked version of the Event Dao. */
    @Mock private lateinit var mockEventDao: EventDao
    /** A mocked version of the Condition Dao. */
    @Mock private lateinit var mockConditionDao: ConditionDao
    /** A mocked version of the End condition Dao. */
    @Mock private lateinit var mockEndConditionDao: EndConditionDao
    /** Object under tests. */
    private lateinit var repository: RepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val mockContext = mock(Context::class.java)
        mockWhen(mockContext.filesDir).thenReturn(File(DATA_FILE_DIR))
        val mockDatabase = mock(ClickDatabase::class.java)
        mockWhen(mockDatabase.scenarioDao()).thenReturn(mockScenarioDao)
        mockWhen(mockDatabase.eventDao()).thenReturn(mockEventDao)
        mockWhen(mockDatabase.conditionDao()).thenReturn(mockConditionDao)
        mockWhen(mockDatabase.endConditionDao()).thenReturn(mockEndConditionDao)

        repository = RepositoryImpl(mockDatabase, mockBitmapManager, mockBackupEngine)
        clearInvocations(mockScenarioDao, mockEventDao, mockConditionDao, mockEndConditionDao)
    }

    @Test
    fun createScenario() = runTest {
        repository.addScenario(TestsData.getNewScenario())
        verify(mockScenarioDao).add(TestsData.getNewScenarioEntity())
        Unit
    }

    @Test
    fun deleteScenario() = runTest {
        mockWhen(mockEventDao.getEventsIds(TestsData.SCENARIO_ID)).thenReturn(emptyList())
        repository.deleteScenario(TestsData.getNewScenario())

        verify(mockScenarioDao).delete(TestsData.getNewScenarioEntity())
    }

    @Test
    fun deleteScenario_removedConditions() = runTest {
        mockWhen(mockEventDao.getEventsIds(TestsData.SCENARIO_ID)).thenReturn(listOf(2L, 4L))
        mockWhen(mockConditionDao.getConditionsPath(2L)).thenReturn(listOf("toto", "tutu"))
        mockWhen(mockConditionDao.getConditionsPath(4L)).thenReturn(listOf("tutu"))
        mockWhen(mockConditionDao.getValidPathCount("toto")).thenReturn(1)
        mockWhen(mockConditionDao.getValidPathCount("tutu")).thenReturn(0)
        repository.deleteScenario(TestsData.getNewScenario())

        verify(mockBitmapManager).deleteBitmaps(listOf("tutu"))
    }

    @Test
    fun getScenarioWithEndConditions_empty() = runTest {
        val scenarioEntity = TestsData.getNewScenarioEntity()
        val scenario = TestsData.getNewScenario()
        mockWhen(mockScenarioDao.getScenarioWithEndConditions(TestsData.SCENARIO_ID)).thenReturn(
            flow {
                emit(ScenarioWithEndConditions(scenarioEntity, emptyList()))
            }
        )

        val result = repository.getScenarioWithEndConditionsFlow(TestsData.SCENARIO_ID).first()
        assertEquals(scenario, result.first)
        assertTrue(result.second.isEmpty())
    }

    @Test
    fun getScenarioWithEndConditions_notEmpty() = runTest {
        val scenarioEntity = TestsData.getNewScenarioEntity()
        val scenario = TestsData.getNewScenario()
        val eventEntity = TestsData.getNewEventEntity(scenarioId = scenarioEntity.id, priority = 1)
        val endConditionWithEvent = TestsData.getNewEndConditionWithEvent(event = eventEntity)

        mockWhen(mockScenarioDao.getScenarioWithEndConditions(TestsData.SCENARIO_ID)).thenReturn(
            flow {
                emit(ScenarioWithEndConditions(scenarioEntity, listOf(endConditionWithEvent)))
            }
        )

        val result = repository.getScenarioWithEndConditionsFlow(TestsData.SCENARIO_ID).first()
        assertEquals(scenario, result.first)
        assertEquals(
            TestsData.getNewEndCondition(),
            result.second[0]
        )
    }

    @Test
    fun getCompleteEventList() = runTest {
        mockWhen(mockEventDao.getCompleteEventsFlow(TestsData.SCENARIO_ID)).thenReturn(
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
            repository.getCompleteEventListFlow(TestsData.SCENARIO_ID).first(),
        )
    }

    @Test
    fun getBitmap() = runTest {
        repository.getBitmap("toto", 20, 100)
        verify(mockBitmapManager).loadBitmap("toto", 20, 100)
        Unit
    }
}

private const val DATA_FILE_DIR = "/toto/titi"