/*
 * Copyright (C) 2024 Kevin Buzeau
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
import android.graphics.Rect
import android.os.Build

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.TutorialDatabase
import com.buzbuz.smartautoclicker.core.database.dao.ConditionDao
import com.buzbuz.smartautoclicker.core.database.dao.EventDao
import com.buzbuz.smartautoclicker.core.database.dao.ScenarioDao
import com.buzbuz.smartautoclicker.core.database.entity.CompleteEventEntity
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.action.ActionTestsData
import com.buzbuz.smartautoclicker.core.domain.model.condition.ConditionTestsData
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.EventTestsData
import com.buzbuz.smartautoclicker.core.domain.model.scenario.ScenarioTestsData

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
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

/** Tests for the [Repository]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class RepositoryTests {

    @Mock private lateinit var mockBitmapManager: BitmapRepository

    @Mock private lateinit var mockDatabase: ClickDatabase
    @Mock private lateinit var mockScenarioDao: ScenarioDao
    @Mock private lateinit var mockEventDao: EventDao
    @Mock private lateinit var mockConditionDao: ConditionDao

    @Mock private lateinit var mockTutoDatabase: TutorialDatabase
    @Mock private lateinit var mockTutoScenarioDao: ScenarioDao
    @Mock private lateinit var mockTutoEventDao: EventDao
    @Mock private lateinit var mockTutoConditionDao: ConditionDao

    /** Object under tests. */
    private lateinit var repository: Repository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val mockContext = mock(Context::class.java)
        mockWhen(mockContext.filesDir).thenReturn(File(DATA_FILE_DIR))

        mockWhen(mockDatabase.scenarioDao()).thenReturn(mockScenarioDao)
        mockWhen(mockDatabase.eventDao()).thenReturn(mockEventDao)
        mockWhen(mockDatabase.conditionDao()).thenReturn(mockConditionDao)

        mockWhen(mockTutoDatabase.scenarioDao()).thenReturn(mockTutoScenarioDao)
        mockWhen(mockTutoDatabase.eventDao()).thenReturn(mockTutoEventDao)
        mockWhen(mockTutoDatabase.conditionDao()).thenReturn(mockTutoConditionDao)

        repository = Repository(mockDatabase, mockTutoDatabase, mockBitmapManager)
        clearInvocations(mockScenarioDao, mockEventDao, mockConditionDao)
    }

    @Test
    fun createScenario() = runTest {
        repository.addScenario(ScenarioTestsData.getNewScenario())
        verify(mockScenarioDao).add(ScenarioTestsData.getNewScenarioEntity())
        Unit
    }

    @Test
    fun deleteScenario() = runTest {
        mockWhen(mockEventDao.getEventsIds(ScenarioTestsData.SCENARIO_ID)).thenReturn(emptyList())
        repository.deleteScenario(Identifier(databaseId = ScenarioTestsData.SCENARIO_ID))

        verify(mockScenarioDao).delete(ScenarioTestsData.SCENARIO_ID)
    }

    @Test
    fun deleteScenario_removedConditions() = runTest {
        mockWhen(mockEventDao.getEventsIds(ScenarioTestsData.SCENARIO_ID)).thenReturn(listOf(2L, 4L))
        mockWhen(mockConditionDao.getConditionsPaths(2L)).thenReturn(listOf("toto", "tutu"))
        mockWhen(mockConditionDao.getConditionsPaths(4L)).thenReturn(listOf("tutu"))
        mockWhen(mockConditionDao.getValidPathCount("toto")).thenReturn(1)
        mockWhen(mockConditionDao.getValidPathCount("tutu")).thenReturn(0)
        repository.deleteScenario(Identifier(databaseId = ScenarioTestsData.SCENARIO_ID))

        verify(mockBitmapManager).deleteImageConditionBitmaps(listOf("tutu"))
    }

    @Test
    fun getCompleteImageEventList() = runTest {
        mockWhen(mockEventDao.getCompleteImageEventsFlow(ScenarioTestsData.SCENARIO_ID)).thenReturn(
            flow {
                emit(listOf(
                    CompleteEventEntity(
                        event = EventTestsData.getNewImageEventEntity(id = EventTestsData.EVENT_ID, scenarioId = EventTestsData.EVENT_SCENARIO_ID),
                        actions = listOf(ActionTestsData.getNewPauseEntity(eventId = EventTestsData.EVENT_ID, priority = 0)),
                        conditions = listOf(ConditionTestsData.getNewImageConditionEntity(eventId = EventTestsData.EVENT_ID))
                    )
                ))
            }
        )

        assertEquals(
            listOf(
                EventTestsData.getNewImageEvent(
                    id = EventTestsData.EVENT_ID,
                    scenarioId = EventTestsData.EVENT_SCENARIO_ID,
                    actions = mutableListOf(ActionTestsData.getNewPause(eventId = EventTestsData.EVENT_ID)),
                    conditions = mutableListOf(ConditionTestsData.getNewImageCondition(eventId = EventTestsData.EVENT_ID))
                ),
            ),
            repository.getScreenEventsFlow(ScenarioTestsData.SCENARIO_ID).first(),
        )
    }

    @Test
    fun getCompleteTriggerEventList() = runTest {
        mockWhen(mockEventDao.getCompleteTriggerEventsFlow(ScenarioTestsData.SCENARIO_ID)).thenReturn(
            flow {
                emit(listOf(
                    CompleteEventEntity(
                        event = EventTestsData.getNewTriggerEventEntity(id = EventTestsData.EVENT_ID, scenarioId = EventTestsData.EVENT_SCENARIO_ID),
                        actions = listOf(ActionTestsData.getNewPauseEntity(eventId = EventTestsData.EVENT_ID, priority = 0)),
                        conditions = listOf(ConditionTestsData.getNewTimerReachedConditionEntity(eventId = EventTestsData.EVENT_ID)),
                    )
                ))
            }
        )

        assertEquals(
            listOf(
                EventTestsData.getNewTriggerEvent(
                    id = EventTestsData.EVENT_ID,
                    scenarioId = EventTestsData.EVENT_SCENARIO_ID,
                    actions = mutableListOf(ActionTestsData.getNewPause(eventId = EventTestsData.EVENT_ID)),
                    conditions = mutableListOf(ConditionTestsData.getNewTimerReachedCondition(eventId = EventTestsData.EVENT_ID)),
                ),
            ),
            repository.getTriggerEventsFlow(ScenarioTestsData.SCENARIO_ID).first(),
        )
    }

    @Test
    fun getBitmap() = runTest {
        repository.getConditionBitmap(
            ImageCondition(
                id = Identifier(databaseId = 1L),
                eventId = Identifier(databaseId = 2L),
                name = "tata",
                threshold = 10,
                detectionType = EXACT,
                shouldBeDetected = true,
                captureArea = Rect(0, 0, 20, 100),
                path = "toto",
                priority = 0,
            )
        )
        verify(mockBitmapManager).getImageConditionBitmap("toto", 20, 100)
        Unit
    }
}

private const val DATA_FILE_DIR = "/toto/titi"