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

import android.graphics.Bitmap
import android.os.Build

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry

import com.buzbuz.smartautoclicker.feature.backup.data.BackupEngine
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapManager
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.scenario.toScenario
import com.buzbuz.smartautoclicker.core.domain.utils.TestsData
import com.buzbuz.smartautoclicker.core.domain.utils.anyNotNull
import com.buzbuz.smartautoclicker.core.domain.utils.assertSameEndConditionNoIdCheck
import com.buzbuz.smartautoclicker.core.domain.utils.assertSameEventListNoIdCheck

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.`when` as mockWhen
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify

import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for the update of scenario with in the database by using the [RepositoryImpl]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioUpdateTests {

    /** In memory database used to test the dao. */
    private lateinit var database: ClickDatabase
    /** Scenario entity inserted before each tests. */
    private lateinit var testScenarioEntity: ScenarioEntity
    /** Object under tests. */
    private lateinit var repository: RepositoryImpl

    /** A mocked version of the bitmap manager. */
    @Mock private lateinit var mockBitmapManager: com.buzbuz.smartautoclicker.core.bitmaps.BitmapManager
    /** A mocked version of the backup engine. */
    @Mock private lateinit var mockBackupEngine: BackupEngine

    private fun createSimpleValidScenario(eventId: Long = TestsData.EVENT_ID): com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedScenario {
        val action = TestsData.getNewClick(id = 0, eventId = eventId)
        val editedEvent = TestsData.getNewEvent(
                id = eventId,
                scenarioId = testScenarioEntity.id,
                conditions = mutableListOf(TestsData.getNewCondition(id = 0, eventId = eventId)),
                actions = mutableListOf(action),
                priority = 0,
            ),
        )

        return com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedScenario(
            scenario = testScenarioEntity.toScenario(),
            endConditions = emptyList(),
            events = listOf(editedEvent),
        )
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        database = Room
            .inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                ClickDatabase::class.java,
            )
            .allowMainThreadQueries()
            .build()

        repository = RepositoryImpl(database, mockBitmapManager, mockBackupEngine)

        // Create a scenario to host all our test events
        runBlocking {
            testScenarioEntity = TestsData.getNewScenarioEntity()
            database.scenarioDao().add(testScenarioEntity)
        }
    }

    @After
    fun tearDown() {
        database.clearAllTables()
        database.close()
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_endConditions_invalidItemId() = runTest {
        var editedScenario = createSimpleValidScenario()
        editedScenario = editedScenario.copy(
            endConditions = listOf(
                com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedEndCondition(
                    endCondition = TestsData.getNewEndCondition(),
                    itemId = com.buzbuz.smartautoclicker.feature.scenario.config.model.INVALID_EDITED_ITEM_ID,
                    eventItemId = editedScenario.events.first().itemId,
                )
            )
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_endConditions_invalidEventItemId() = runTest {
        var editedScenario = createSimpleValidScenario()
        editedScenario = editedScenario.copy(
            endConditions = listOf(
                com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedEndCondition(
                    endCondition = TestsData.getNewEndCondition(),
                    itemId = 1,
                    eventItemId = com.buzbuz.smartautoclicker.feature.scenario.config.model.INVALID_EDITED_ITEM_ID,
                )
            )
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_endConditions_unknownEventItemId() = runTest {
        var editedScenario = createSimpleValidScenario()
        editedScenario = editedScenario.copy(
            endConditions = listOf(
                com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedEndCondition(
                    endCondition = TestsData.getNewEndCondition(),
                    itemId = 1,
                    eventItemId = editedScenario.events.first().itemId + 1,
                )
            )
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_events_empty() = runTest {
        val editedScenario = createSimpleValidScenario().copy(
            events = emptyList()
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_events_itemId() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(itemId = com.buzbuz.smartautoclicker.feature.scenario.config.model.INVALID_EDITED_ITEM_ID))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_conditions_empty() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                event = editedEvent.event.copy(
                    conditions = mutableListOf(),
                )
            ))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_actions_emptyEditedList() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                editedActions = emptyList()
            ))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_actions_emptyList() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                event = editedEvent.event.copy(
                    actions = mutableListOf(),
                )
            ))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_actions_editedActualNotSameSize() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                event = editedEvent.event.copy(
                    actions = editedEvent.event.actions?.toMutableList()?.apply {
                        add(TestsData.getNewPause(eventId = editedEvent.event.id))
                    },
                )
            ))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_click_incomplete() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()
        val incompleteClick = TestsData.getNewClick(
            eventId = editedEvent.event.id,
            pressDuration = null,
        )

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                editedActions = listOf(
                    com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedAction(
                        itemId = 0,
                        action = incompleteClick
                    )
                ),
                event = editedEvent.event.copy(
                    actions = mutableListOf(incompleteClick),
                )
            ))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_swipe_incomplete() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()
        val incompleteSwipe = TestsData.getNewSwipe(
            eventId = editedEvent.event.id,
            swipeDuration = null,
        )

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                editedActions = listOf(
                    com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedAction(
                        itemId = 0,
                        action = incompleteSwipe
                    )
                ),
                event = editedEvent.event.copy(
                    actions = mutableListOf(incompleteSwipe),
                )
            ))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_pause_incomplete() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()
        val incompletePause = TestsData.getNewPause(
            eventId = editedEvent.event.id,
            pauseDuration = null,
        )

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                editedActions = listOf(
                    com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedAction(
                        itemId = 0,
                        action = incompletePause
                    )
                ),
                event = editedEvent.event.copy(
                    actions = mutableListOf(incompletePause),
                )
            ))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_intent_incomplete() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()
        val incompleteIntent = TestsData.getNewIntent(
            eventId = editedEvent.event.id,
            name = null,
        )

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                editedActions = listOf(
                    com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedAction(
                        itemId = 0,
                        action = incompleteIntent
                    )
                ),
                event = editedEvent.event.copy(
                    actions = mutableListOf(incompleteIntent),
                )
            ))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_toggleEvent_incomplete() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()
        val incompleteToggleEvent = TestsData.getNewToggleEvent(
            eventId = editedEvent.event.id,
            name = null,
        )

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                editedActions = listOf(
                    com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedAction(
                        itemId = 0,
                        action = incompleteToggleEvent
                    )
                ),
                event = editedEvent.event.copy(
                    actions = mutableListOf(incompleteToggleEvent),
                )
            ))
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_toggleEvent_invalidEventItemId() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()
        val invalidToggleEvent = TestsData.getNewToggleEvent(
            eventId = editedEvent.event.id,
        )

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                event = editedEvent.event.copy(actions = mutableListOf(invalidToggleEvent)),
                editedActions = listOf(
                    com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedAction(
                        itemId = 0,
                        action = invalidToggleEvent,
                        toggleEventItemId = com.buzbuz.smartautoclicker.feature.scenario.config.model.INVALID_EDITED_ITEM_ID
                    )
                )
            )),
        )

        repository.updateScenario(editedScenario)
    }

    @Test(expected = IllegalArgumentException::class)
    fun updateScenario_invalid_toggleEvent_unknownEventItemId() = runTest {
        var editedScenario = createSimpleValidScenario()
        val editedEvent = editedScenario.events.first()
        val toggleEvent = TestsData.getNewToggleEvent(
            eventId = editedEvent.event.id,
        )

        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                event = editedEvent.event.copy(actions = mutableListOf(toggleEvent)),
                editedActions = listOf(
                    com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedAction(
                        itemId = 0,
                        action = toggleEvent,
                        toggleEventItemId = editedEvent.itemId + 1
                    )
                )
            )),
        )

        repository.updateScenario(editedScenario)
    }

    @Test
    fun updateScenario_valid_insertEndCondition() = runTest {
        var editedScenario = createSimpleValidScenario(0)
        val endCondition = TestsData.getNewEndCondition(id = 0, scenarioId = editedScenario.scenario.id)
        editedScenario = editedScenario.copy(
            endConditions = listOf(
                com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedEndCondition(
                    endCondition = endCondition,
                    itemId = 1,
                    eventItemId = editedScenario.events.first().itemId,
                )
            )
        )
        repository.updateScenario(editedScenario)

        val dbEndCondition = repository.getScenarioWithEndConditionsFlow(editedScenario.scenario.id).first().second.first()
        assertSameEndConditionNoIdCheck(endCondition, dbEndCondition)
    }

    @Test
    fun updateScenario_valid_insertEndCondition_eventIdMapping() = runTest {
        var editedScenario = createSimpleValidScenario(0)
        val endCondition = TestsData.getNewEndCondition(id = 0, scenarioId = editedScenario.scenario.id)
        editedScenario = editedScenario.copy(
            endConditions = listOf(
                com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedEndCondition(
                    endCondition = endCondition,
                    itemId = 1,
                    eventItemId = editedScenario.events.first().itemId,
                )
            )
        )
        repository.updateScenario(editedScenario)

        val dbEvent = repository.getCompleteEventList(editedScenario.scenario.id).first()
        val dbEndCondition = repository.getScenarioWithEndConditionsFlow(editedScenario.scenario.id).first().second.first()
        assertEquals(
            "Event id is incorrect",
            dbEvent.id,
            dbEndCondition.eventId,
        )
    }

    @Test
    fun updateScenario_valid_insertCompleteEvent() = runTest {
        val editedScenario = createSimpleValidScenario(0)
        repository.updateScenario(editedScenario)

        assertSameEventListNoIdCheck(
            editedScenario.events.map { editedEvent -> editedEvent.event },
            repository.getCompleteEventList(editedScenario.scenario.id)
        )
    }

    @Test
    fun updateScenario_valid_insertToggleEvent_eventIdMapping() = runTest {
        var editedScenario = createSimpleValidScenario(0)
        val editedEvent = editedScenario.events.first()
        val toggleEvent = TestsData.getNewToggleEvent(
            id = 0,
            eventId = editedEvent.event.id,
        )
        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                event = editedEvent.event.copy(actions = mutableListOf(toggleEvent)),
                editedActions = listOf(
                    com.buzbuz.smartautoclicker.feature.scenario.config.model.EditedAction(
                        itemId = 0,
                        action = toggleEvent,
                        toggleEventItemId = editedEvent.itemId
                    )
                )
            )),
        )
        repository.updateScenario(editedScenario)

        val dbEvent = repository.getCompleteEventList(editedScenario.scenario.id).first()
        val dbToggleEvent = dbEvent.actions?.first()
        if (dbToggleEvent is Action.ToggleEvent) {
            assertEquals(
                "Event id is incorrect",
                dbEvent.id,
                dbToggleEvent.toggleEventId,
            )
        } else {
            fail("Action is not a toggle event")
        }
    }

    @Test
    fun updateScenario_valid_saveBitmap() = runTest {
        val bitmap = mock(Bitmap::class.java)
        val path = "toto"
        mockWhen(mockBitmapManager.saveBitmap(bitmap)).thenReturn(path)

        var editedScenario = createSimpleValidScenario(0)
        val editedEvent = editedScenario.events.first()
        editedScenario = editedScenario.copy(
            events = listOf(editedEvent.copy(
                event = editedEvent.event.copy(
                    conditions = mutableListOf(
                        TestsData.getNewCondition(id= 0, eventId = 0, path = null, bitmap = bitmap)
                    )
                )
            ))
        )
        repository.updateScenario(editedScenario)

        verify(mockBitmapManager).saveBitmap(bitmap)
        verify(mockBitmapManager, never()).deleteBitmaps(anyNotNull())
        assertEquals(
            "Condition bitmap path is invalid",
            path,
            repository.getCompleteEventList(editedScenario.scenario.id).first().conditions?.first()?.path
        )
    }
}