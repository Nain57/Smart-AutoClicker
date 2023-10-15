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
package com.buzbuz.smartautoclicker.core.domain

import android.content.Context
import android.os.Build

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapManager
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.TutorialDatabase
import com.buzbuz.smartautoclicker.core.database.dao.ConditionDao
import com.buzbuz.smartautoclicker.core.database.dao.EndConditionDao
import com.buzbuz.smartautoclicker.core.database.dao.EventDao
import com.buzbuz.smartautoclicker.core.database.dao.ScenarioDao
import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioWithEndConditions
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.utils.TestsData

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

    @Mock private lateinit var mockBitmapManager: BitmapManager

    @Mock private lateinit var mockDatabase: ClickDatabase
    @Mock private lateinit var mockScenarioDao: ScenarioDao
    @Mock private lateinit var mockEventDao: EventDao
    @Mock private lateinit var mockConditionDao: ConditionDao
    @Mock private lateinit var mockEndConditionDao: EndConditionDao

    @Mock private lateinit var mockTutoDatabase: TutorialDatabase
    @Mock private lateinit var mockTutoScenarioDao: ScenarioDao
    @Mock private lateinit var mockTutoEventDao: EventDao
    @Mock private lateinit var mockTutoConditionDao: ConditionDao
    @Mock private lateinit var mockTutoEndConditionDao: EndConditionDao

    /** Object under tests. */
    private lateinit var repository: RepositoryImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val mockContext = mock(Context::class.java)
        mockWhen(mockContext.filesDir).thenReturn(File(DATA_FILE_DIR))

        mockWhen(mockDatabase.scenarioDao()).thenReturn(mockScenarioDao)
        mockWhen(mockDatabase.eventDao()).thenReturn(mockEventDao)
        mockWhen(mockDatabase.conditionDao()).thenReturn(mockConditionDao)
        mockWhen(mockDatabase.endConditionDao()).thenReturn(mockEndConditionDao)

        mockWhen(mockTutoDatabase.scenarioDao()).thenReturn(mockTutoScenarioDao)
        mockWhen(mockTutoDatabase.eventDao()).thenReturn(mockTutoEventDao)
        mockWhen(mockTutoDatabase.conditionDao()).thenReturn(mockTutoConditionDao)
        mockWhen(mockTutoDatabase.endConditionDao()).thenReturn(mockTutoEndConditionDao)

        repository = RepositoryImpl(mockDatabase, mockTutoDatabase, mockBitmapManager)
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
        repository.deleteScenario(Identifier(databaseId = TestsData.SCENARIO_ID))

        verify(mockScenarioDao).delete(TestsData.SCENARIO_ID)
    }

    @Test
    fun deleteScenario_removedConditions() = runTest {
        mockWhen(mockEventDao.getEventsIds(TestsData.SCENARIO_ID)).thenReturn(listOf(2L, 4L))
        mockWhen(mockConditionDao.getConditionsPath(2L)).thenReturn(listOf("toto", "tutu"))
        mockWhen(mockConditionDao.getConditionsPath(4L)).thenReturn(listOf("tutu"))
        mockWhen(mockConditionDao.getValidPathCount("toto")).thenReturn(1)
        mockWhen(mockConditionDao.getValidPathCount("tutu")).thenReturn(0)
        repository.deleteScenario(Identifier(databaseId = TestsData.SCENARIO_ID))

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
            repository.getEventsFlow(TestsData.SCENARIO_ID).first(),
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