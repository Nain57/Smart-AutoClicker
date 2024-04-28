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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


/** Tests for the [QualityManager]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class QualityManagerTests {

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
        val qualityManager = QualityManager(testDataStore, testDispatcher)
        assertEquals(Quality.Unknown, qualityManager.quality.value)
    }

    @Test
    fun `first start state`() = runTest(testDispatcher) {
        val qualityManager = QualityManager(testDataStore, testDispatcher)

        qualityManager.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.FirstTime, qualityManager.quality.value)
    }

    @Test
    fun `restart crash state`() = runTest(testDispatcher) {
        testDataStore.setLowQualityConditions()
        val qualityManager = QualityManager(testDataStore, testDispatcher)

        qualityManager.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.Low, qualityManager.quality.value)
    }

    @Test
    fun `restart permission removed state`() = runTest(testDispatcher) {
        testDataStore.setMediumQualityConditions()
        val qualityManager = QualityManager(testDataStore, testDispatcher)

        qualityManager.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.Medium, qualityManager.quality.value)
    }

    @Test
    fun `grace time medium quality`() = runTest(testDispatcher) {
        testDataStore.setMediumQualityConditions()
        val qualityManager = QualityManager(testDataStore, testDispatcher)

        qualityManager.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.Medium, qualityManager.quality.value)
        testDispatcher.scheduler.advanceTimeBy(
            Quality.Medium.backToHighDelay!!.inWholeMilliseconds + 1000
        )
        assertEquals(Quality.High, qualityManager.quality.value)
    }

    @Test
    fun `grace time low quality`() = runTest(testDispatcher) {
        testDataStore.setLowQualityConditions()
        val qualityManager = QualityManager(testDataStore, testDispatcher)

        qualityManager.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.Low, qualityManager.quality.value)
        testDispatcher.scheduler.advanceTimeBy(
            Quality.Low.backToHighDelay!!.inWholeMilliseconds + 1000
        )
        assertEquals(Quality.High, qualityManager.quality.value)
    }

    @Test
    fun `grace time first time quality`() = runTest(testDispatcher) {
        val qualityManager = QualityManager(testDataStore, testDispatcher)

        qualityManager.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertEquals(Quality.FirstTime, qualityManager.quality.value)
        testDispatcher.scheduler.advanceTimeBy(
            Quality.FirstTime.backToHighDelay!!.inWholeMilliseconds + 1000
        )
        assertEquals(Quality.High, qualityManager.quality.value)
    }

    @Test
    fun `service connected time is set`() = runTest(testDispatcher) {
        val qualityManager = QualityManager(testDataStore, testDispatcher)

        qualityManager.onServiceConnected()
        testDispatcher.scheduler.runCurrent()

        assertNotEquals(
            INVALID_TIME,
            testDataStore.flowOf(KEY_LAST_SERVICE_START_TIME_MS, INVALID_TIME).first()
        )
    }

    @Test
    fun `service foreground time is set`() = runTest(testDispatcher) {
        val qualityManager = QualityManager(testDataStore, testDispatcher)

        qualityManager.onServiceConnected()
        qualityManager.onServiceForegroundStart()
        testDispatcher.scheduler.runCurrent()

        assertNotEquals(
            INVALID_TIME,
            testDataStore.flowOf(KEY_LAST_SERVICE_FOREGROUND_TIME_MS, INVALID_TIME).first()
        )
    }

    @Test
    fun `service foreground time is reset`() = runTest(testDispatcher) {
        val qualityManager = QualityManager(testDataStore, testDispatcher)

        qualityManager.onServiceConnected()
        qualityManager.onServiceForegroundStart()
        qualityManager.onServiceForegroundEnd()
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
}
