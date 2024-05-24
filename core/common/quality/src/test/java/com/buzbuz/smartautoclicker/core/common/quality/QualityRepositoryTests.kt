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
package com.buzbuz.smartautoclicker.core.common.quality

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.test.core.app.ApplicationProvider

import com.buzbuz.smartautoclicker.core.common.quality.data.INVALID_TIME
import com.buzbuz.smartautoclicker.core.common.quality.data.KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT
import com.buzbuz.smartautoclicker.core.common.quality.data.KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT
import com.buzbuz.smartautoclicker.core.common.quality.data.KEY_LAST_SERVICE_FOREGROUND_TIME_MS
import com.buzbuz.smartautoclicker.core.common.quality.data.KEY_LAST_SERVICE_START_TIME_MS
import com.buzbuz.smartautoclicker.core.common.quality.domain.Quality
import com.buzbuz.smartautoclicker.core.common.quality.domain.QualityMetricsMonitor
import com.buzbuz.smartautoclicker.core.common.quality.domain.QualityRepository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


/** Tests for the [QualityRepository]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class QualityRepositoryTests {

    private val testContext: Context = ApplicationProvider.getApplicationContext()
    private val testDispatcher = StandardTestDispatcher()
    private val testCoroutineScope = TestScope(testDispatcher + Job())
    private val testDataStore: DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = testCoroutineScope,
            produceFile = { testContext.preferencesDataStoreFile("TEST_DATASTORE_NAME") }
        )

    @Mock private lateinit var mockActivity: FragmentActivity
    @Mock private lateinit var mockFragmentManager: FragmentManager
    @Mock private lateinit var mockFragmentTransaction: FragmentTransaction

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mockActivity.supportFragmentManager).thenReturn(mockFragmentManager)
        Mockito.`when`(mockFragmentManager.beginTransaction()).thenReturn(mockFragmentTransaction)
    }

    @Test
    fun `no init`() = runTest(testDispatcher) {
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val qualityRepository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        assertEquals(Quality.Unknown, qualityRepository.quality.value)
    }

    @Test
    fun `first start state`() = runTest(testDispatcher) {
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val qualityRepository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.FirstTime, qualityRepository.quality.value)
    }

    @Test
    fun `restart crash state`() = runTest(testDispatcher) {
        testDataStore.setCrashedQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val qualityRepository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.Crashed, qualityRepository.quality.value)
    }

    @Test
    fun `restart permission removed state`() = runTest(testDispatcher) {
        testDataStore.setExternalIssueQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val qualityRepository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.ExternalIssue, qualityRepository.quality.value)
    }

    @Test
    fun `grace time external issues quality`() = runTest(testDispatcher) {
        testDataStore.setExternalIssueQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val qualityRepository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()

        testDispatcher.scheduler.advanceTimeBy(
            Quality.ExternalIssue.backToHighDelay!!.inWholeMilliseconds - 1000
        )
        assertEquals(Quality.ExternalIssue, qualityRepository.quality.value)
        testDispatcher.scheduler.advanceTimeBy(2000)
        assertEquals(Quality.High, qualityRepository.quality.value)
    }

    @Test
    fun `grace time crashed quality`() = runTest(testDispatcher) {
        testDataStore.setCrashedQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val qualityRepository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()

        testDispatcher.scheduler.advanceTimeBy(
            Quality.Crashed.backToHighDelay!!.inWholeMilliseconds - 1000
        )
        assertEquals(Quality.Crashed, qualityRepository.quality.value)
        testDispatcher.scheduler.advanceTimeBy(2000)
        assertEquals(Quality.High, qualityRepository.quality.value)
    }

    @Test
    fun `grace time first time quality`() = runTest(testDispatcher) {
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val qualityRepository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()

        testDispatcher.scheduler.advanceTimeBy(
            Quality.FirstTime.backToHighDelay!!.inWholeMilliseconds - 1000
        )
        assertEquals(Quality.FirstTime, qualityRepository.quality.value)
        testDispatcher.scheduler.advanceTimeBy(2000)
        assertEquals(Quality.High, qualityRepository.quality.value)
    }


    @Test
    fun `service connected loss count is incremented`() = runTest(testDispatcher) {
        testDataStore.setExternalIssueQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(
            1,
            testDataStore.flowOf(KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT).first()
        )
    }

    @Test
    fun `service connected loss count is not incremented on crash`() = runTest(testDispatcher) {
        testDataStore.setCrashedQualityConditions()
        testDataStore.setTroubleshootingConditions(lossCount = 1, displayCount = 0)
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(
            1,
            testDataStore.flowOf(KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT).first()
        )
    }

    @Test
    fun `service connected time is set`() = runTest(testDispatcher) {
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertNotEquals(
            INVALID_TIME,
            testDataStore.flowOf(KEY_LAST_SERVICE_START_TIME_MS).first()
        )
    }

    @Test
    fun `service foreground time is set`() = runTest(testDispatcher) {
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        metricsMonitor.onServiceForegroundStart()
        testDispatcher.scheduler.runCurrent()

        assertNotEquals(
            INVALID_TIME,
            testDataStore.flowOf(KEY_LAST_SERVICE_FOREGROUND_TIME_MS).first()
        )
    }

    @Test
    fun `service foreground time is reset`() = runTest(testDispatcher) {
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        metricsMonitor.onServiceForegroundStart()
        metricsMonitor.onServiceForegroundEnd()
        testDispatcher.scheduler.runCurrent()

        assertEquals(
            INVALID_TIME,
            testDataStore.flowOf(KEY_LAST_SERVICE_FOREGROUND_TIME_MS).first()
        )
    }

    @Test
    fun `show troubleshooting dialog when needed`() = runTest(testDispatcher) {
        testDataStore.setExternalIssueQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val repository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)
        testDispatcher.scheduler.runCurrent()
        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        repository.startTroubleshootingUiFlowIfNeeded(mockActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the show dialog and the data store increment
        inOrder(mockFragmentManager, mockFragmentTransaction) {
            verify(mockFragmentManager).beginTransaction()
            verify(mockFragmentTransaction).commit()
        }
        assertEquals(1, testDataStore.flowOf(KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT).first())
    }

    @Test
    fun `show troubleshooting dialog twice skip second`() = runTest(testDispatcher) {
        testDataStore.setExternalIssueQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val repository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)
        testDispatcher.scheduler.runCurrent()
        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        repository.startTroubleshootingUiFlowIfNeeded(mockActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()
        repository.startTroubleshootingUiFlowIfNeeded(mockActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify the show dialog and the data store increment
        inOrder(mockFragmentManager, mockFragmentTransaction) {
            verify(mockFragmentManager).beginTransaction()
            verify(mockFragmentTransaction).commit()
        }
        assertEquals(1, testDataStore.flowOf(KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT).first())
    }

    @Test
    fun `show troubleshooting dialog skipped on crash`() = runTest(testDispatcher) {
        testDataStore.setCrashedQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val repository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)
        testDispatcher.scheduler.runCurrent()
        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        repository.startTroubleshootingUiFlowIfNeeded(mockActivity) {}
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockFragmentManager, never()).beginTransaction()
        verify(mockFragmentTransaction, never()).commit()
        assertEquals(null, testDataStore.flowOf(KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT).first())
    }

    private suspend fun DataStore<Preferences>.setExternalIssueQualityConditions() =
        edit { preferences ->
            preferences[KEY_LAST_SERVICE_START_TIME_MS] = 1
            preferences.remove(KEY_LAST_SERVICE_FOREGROUND_TIME_MS)
        }

    private suspend fun DataStore<Preferences>.setCrashedQualityConditions() =
        edit { preferences ->
            preferences[KEY_LAST_SERVICE_START_TIME_MS] = 1
            preferences[KEY_LAST_SERVICE_FOREGROUND_TIME_MS] = 2
        }

    private suspend fun DataStore<Preferences>.setTroubleshootingConditions(lossCount: Int, displayCount: Int) =
        edit { preferences ->
            preferences[KEY_ACCESSIBILITY_SERVICE_PERMISSION_LOSS_COUNT] = lossCount
            preferences[KEY_ACCESSIBILITY_SERVICE_TROUBLESHOOTING_DIALOG_COUNT] = displayCount
        }

    private fun <T> DataStore<Preferences>.flowOf(key: Preferences.Key<T>): Flow<T?> =
        data.map { preferences -> preferences[key] }
}
