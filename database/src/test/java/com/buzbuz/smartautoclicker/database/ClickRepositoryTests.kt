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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.database.room.ClickDao
import com.buzbuz.smartautoclicker.database.room.ClickDatabase
import com.buzbuz.smartautoclicker.database.room.ClickWithConditions
import com.buzbuz.smartautoclicker.database.room.ConditionEntity
import com.buzbuz.smartautoclicker.database.room.ScenarioEntity
import com.buzbuz.smartautoclicker.database.room.ScenarioWithClicks
import com.buzbuz.smartautoclicker.database.utils.TestsData
import com.buzbuz.smartautoclicker.database.utils.anyNotNull
import com.buzbuz.smartautoclicker.database.utils.getOrAwaitValue

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as mockWhen

import org.robolectric.annotation.Config

/** Tests the [ClickRepository] class. */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
internal class ClickRepositoryTests {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    /** Coroutine dispatcher for the tests. */
    private val testDispatcher = TestCoroutineDispatcher()
    /** Coroutine scope for the tests. */
    private val testScope = TestCoroutineScope(testDispatcher)

    /** LiveData returned by the dao containing the tests data scenarios. */
    private val databaseScenarioList = MutableLiveData<List<ScenarioWithClicks>>()
    /** LiveData returned by the dao containing the tests data clickless conditions. */
    private val databaseClicklessConditions = MutableLiveData<List<ConditionEntity>>()
    /** A mocked version of the Dao. */
    private lateinit var mockDao: ClickDao
    /** Object under tests. */
    private lateinit var repository: ClickRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        val mockDatabase = mock(ClickDatabase::class.java)
        mockDao = mock(ClickDao::class.java)
        mockWhen(mockDatabase.clickDao()).thenReturn(mockDao)
        mockWhen(mockDao.getClickScenarios()).thenReturn(databaseScenarioList)
        mockWhen(mockDao.getClicklessConditions()).thenReturn(databaseClicklessConditions)

        repository = ClickRepository(mockDatabase)
        clearInvocations(mockDao)
    }

    @After
    fun tearDown() {
        testScope.cleanupTestCoroutines()
        Dispatchers.resetMain()
    }

    @Test
    fun createScenario() = runBlocking {
        repository.createScenario(TestsData.SCENARIO_NAME)

        verify(mockDao).addClickScenario(ScenarioEntity(0, TestsData.SCENARIO_NAME))
    }

    @Test
    fun renameScenario() = runBlocking {
        repository.renameScenario(TestsData.SCENARIO_ID, TestsData.SCENARIO_NAME)
        verify(mockDao).renameScenario(TestsData.SCENARIO_ID, TestsData.SCENARIO_NAME)
    }

    @Test
    fun deleteScenario() = runBlocking {
        val deletedScenario = ClickScenario(TestsData.SCENARIO_ENTITY.name, TestsData.SCENARIO_ENTITY.id)
        repository.deleteScenario(deletedScenario)

        verify(mockDao).deleteClickScenario(TestsData.SCENARIO_ENTITY)
    }

    @Test
    fun getClickList() = runBlocking {
        val clicks = listOf(
            ClickWithConditions(TestsData.CLICK_ENTITY, listOf(TestsData.CONDITION_ENTITY)),
            ClickWithConditions(TestsData.CLICK_ENTITY_2, listOf(TestsData.CONDITION_ENTITY_2))
        )
        mockWhen(mockDao.getClicksWithConditionsList(TestsData.SCENARIO_ID)).thenReturn(clicks)
        val expected = listOf(
            TestsData.newClickInfo(conditions = listOf(TestsData.CONDITION)),
            TestsData.newClickInfo2(conditions = listOf(TestsData.CONDITION_2))
        )

        assertEquals(expected, repository.getClickList(TestsData.SCENARIO_ID))
    }

    @Test
    fun addClickToEmptyList() = runBlocking {
        val addedClick = TestsData.newClickInfo(0, 0)
        mockWhen(mockDao.getClickCount(addedClick.scenarioId)).thenReturn(0)

        repository.addClick(addedClick)

        verify(mockDao).addClickWithConditions(addedClick.toEntity())
    }

    @Test
    fun addClickToList() = runBlocking {
        val clickCount = 10
        val addedClick = TestsData.newClickInfo(0, 0)
        mockWhen(mockDao.getClickCount(addedClick.scenarioId)).thenReturn(clickCount)

        repository.addClick(addedClick)

        val expected = addedClick.toEntity().apply { click.priority = clickCount }
        verify(mockDao).addClickWithConditions(expected)
    }

    @Test
    fun addClickInvalidId() = runBlocking {
        repository.addClick(TestsData.newClickInfo(158, 0))
        verifyNoInteractions(mockDao)
    }

    @Test
    fun addClickInvalidPriority() = runBlocking {
        repository.addClick(TestsData.newClickInfo(0, 18))
        verifyNoInteractions(mockDao)
    }

    @Test
    fun updateClick() = runBlocking {
        val updatedClick = TestsData.newClickInfo()

        repository.updateClick(updatedClick)
        verify(mockDao).updateClickWithConditions(updatedClick.toEntity())
    }

    @Test
    fun updateClickInvalidId() = runBlocking {
        repository.updateClick(TestsData.newClickInfo(0))
        verifyNoInteractions(mockDao)
    }

    @Test
    fun updateClicksPriorityEmpty() = runBlocking {
        repository.updateClicksPriority(emptyList())
        verifyNoInteractions(mockDao)
    }

    @Test
    fun updateClicksPriorityInvalidSize() = runBlocking {
        val updatedClick = TestsData.newClickInfo()
        mockWhen(mockDao.getClickCount(updatedClick.scenarioId)).thenReturn(42)

        repository.updateClicksPriority(listOf(updatedClick))

        verify(mockDao, never()).updateClicks(anyNotNull())
    }

    @Test
    fun updateClicksPriority() = runBlocking {
        val updatedClicks = listOf(
            TestsData.newClickInfo(0, 2),
            TestsData.newClickInfo(1, 1),
            TestsData.newClickInfo(2, 0)
        )
        mockWhen(mockDao.getClickCount(updatedClicks[0].scenarioId)).thenReturn(updatedClicks.size)

        repository.updateClicksPriority(updatedClicks)

        verify(mockDao).updateClicks(listOf(
            TestsData.newClickInfo(0, 0).toEntity().click,
            TestsData.newClickInfo(1, 1).toEntity().click,
            TestsData.newClickInfo(2, 2).toEntity().click
        ))
    }

    @Test
    fun deleteLessPrioritizedClick() = runBlocking {
        val deletedClick = TestsData.newClickInfo()
        mockWhen(mockDao.getClicksLessPrioritized(deletedClick.scenarioId, deletedClick.priority))
            .thenReturn(emptyList())

        repository.deleteClick(deletedClick)

        verify(mockDao).deleteClick(deletedClick.toEntity().click)
    }

    @Test
    fun deleteHighPriorityClick() = runBlocking {
        val deletedClick = TestsData.newClickInfo(priority = 1)
        val lowerClick1 = TestsData.newClickInfo(priority = 2).toEntity().click
        val lowerClick2 =  TestsData.newClickInfo(priority = 3).toEntity().click
        mockWhen(mockDao.getClicksLessPrioritized(deletedClick.scenarioId, deletedClick.priority))
            .thenReturn(listOf(lowerClick1.copy(), lowerClick2.copy()))

        lowerClick1.apply { priority -= 1 }
        lowerClick2.apply { priority -= 1 }
        repository.deleteClick(deletedClick)

        verify(mockDao).updateClicks(listOf(lowerClick1, lowerClick2))
        verify(mockDao).deleteClick(deletedClick.toEntity().click)
    }

    @Test
    fun deleteClickInvalidId() = runBlocking {
        repository.deleteClick(TestsData.newClickInfo(0))
        verifyNoInteractions(mockDao)
    }

    @Test
    fun deleteClicklessConditions() = runBlocking {
        repository.deleteClicklessConditions()
        verify(mockDao).deleteClicklessConditions()
    }

    @Test
    fun getScenarios() = runBlocking {
        val scenarios = listOf(
            ScenarioWithClicks(TestsData.SCENARIO_ENTITY_2, emptyList()),
            ScenarioWithClicks(TestsData.SCENARIO_ENTITY, emptyList())
        )
        databaseScenarioList.value = scenarios

        val expected = listOf(
            ClickScenario(TestsData.SCENARIO_ENTITY_2.name, TestsData.SCENARIO_ENTITY_2.id),
            ClickScenario(TestsData.SCENARIO_ENTITY.name, TestsData.SCENARIO_ENTITY.id)
        )
        assertEquals(expected, repository.scenarios.getOrAwaitValue())
    }

    @Test
    fun getClicklessConditions() = runBlocking {
        val conditions = listOf(TestsData.CONDITION_ENTITY, TestsData.CONDITION_ENTITY_2)
        databaseClicklessConditions.value = conditions

        val expected = listOf(TestsData.CONDITION, TestsData.CONDITION_2)
        assertEquals(expected, repository.clicklessConditions.getOrAwaitValue())
    }

    @Test
    fun getClicks() = runBlocking {
        val clicks = listOf(
            TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ID, 0),
            TestsData.newClickWithConditionEntity(TestsData.SCENARIO_ID, 1)
        )
        val databaseClickList = MutableLiveData(clicks)
        mockWhen(mockDao.getClicksWithConditions(TestsData.SCENARIO_ID)).thenReturn(databaseClickList)

        val expected = listOf(TestsData.newClickInfo(0), TestsData.newClickInfo(1))
        assertEquals(expected, repository.getClicks(TestsData.SCENARIO_ID).getOrAwaitValue())
    }
}