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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider

import com.buzbuz.smartautoclicker.core.common.quality.data.INVALID_TIME
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
import org.junit.Test
import org.junit.runner.RunWith
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
        testDataStore.setLowQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val qualityRepository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.Crashed, qualityRepository.quality.value)
    }

    @Test
    fun `restart permission removed state`() = runTest(testDispatcher) {
        testDataStore.setMediumQualityConditions()
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        val qualityRepository = QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.ExternalIssue, qualityRepository.quality.value)
    }

    @Test
    fun `grace time medium quality`() = runTest(testDispatcher) {
        testDataStore.setMediumQualityConditions()
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
    fun `grace time low quality`() = runTest(testDispatcher) {
        testDataStore.setLowQualityConditions()
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
    fun `service connected time is set`() = runTest(testDispatcher) {
        val metricsMonitor = QualityMetricsMonitor(testDataStore, testDispatcher)
        QualityRepository(metricsMonitor, testDispatcher, testDispatcher)

        metricsMonitor.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertNotEquals(
            INVALID_TIME,
            testDataStore.flowOf(KEY_LAST_SERVICE_START_TIME_MS, INVALID_TIME).first()
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
            testDataStore.flowOf(KEY_LAST_SERVICE_FOREGROUND_TIME_MS, INVALID_TIME).first()
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
            testDataStore.flowOf(KEY_LAST_SERVICE_FOREGROUND_TIME_MS, INVALID_TIME).first()
        )
    }

    private suspend fun DataStore<Preferences>.setMediumQualityConditions() =
        edit { preferences ->
            preferences[KEY_LAST_SERVICE_START_TIME_MS] = 1
            preferences.remove(KEY_LAST_SERVICE_FOREGROUND_TIME_MS)
        }

    private suspend fun DataStore<Preferences>.setLowQualityConditions() =
        edit { preferences ->
            preferences[KEY_LAST_SERVICE_START_TIME_MS] = 1
            preferences[KEY_LAST_SERVICE_FOREGROUND_TIME_MS] = 2
        }

    private fun <T> DataStore<Preferences>.flowOf(key: Preferences.Key<T>, default: T): Flow<T> =
        data.map { preferences -> preferences[key] ?: default }
}
