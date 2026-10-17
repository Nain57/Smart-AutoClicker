/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.quality.domain

import android.content.Context
import android.os.Build

import androidx.test.core.app.ApplicationProvider

import com.buzbuz.smartautoclicker.core.common.quality.data.INVALID_TIME
import com.buzbuz.smartautoclicker.core.common.quality.data.QualityDataSource
import com.buzbuz.smartautoclicker.core.common.quality.data.QualityMetrics

import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class QualityRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockQualityDataSource: QualityDataSource = mockk()
    private val qualityMetricsFlow = MutableStateFlow(METRICS_FIRST_TIME)

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        every { mockQualityDataSource.qualityMetrics() } returns qualityMetricsFlow
    }

    private fun createRepository(monitor: QualityMetricsMonitor): QualityRepository =
        QualityRepository(context, monitor, testDispatcher, testDispatcher)

    private fun createMonitor(): QualityMetricsMonitor =
        QualityMetricsMonitor(mockQualityDataSource, testDispatcher)

    @Test
    fun `quality is not stuck on Unknown when metrics are read before the repository subscribes`() = testScope.runTest {
        qualityMetricsFlow.value = METRICS_EXTERNAL_ISSUE
        val monitor = createMonitor()
        // The eager sharing reads the data source before the repository exists.
        advanceUntilIdle()

        val repository = createRepository(monitor)
        runCurrent()

        assertEquals(Quality.ExternalIssue, repository.quality.value)
    }

    @Test
    fun `quality is FirstTime when service never started`() = testScope.runTest {
        qualityMetricsFlow.value = METRICS_FIRST_TIME
        val repository = createRepository(createMonitor())
        runCurrent()

        assertEquals(Quality.FirstTime, repository.quality.value)
    }

    @Test
    fun `quality is Crashed when a scenario was running`() = testScope.runTest {
        qualityMetricsFlow.value = METRICS_CRASHED
        val repository = createRepository(createMonitor())
        runCurrent()

        assertEquals(Quality.Crashed, repository.quality.value)
    }

    @Test
    fun `quality is ExternalIssue when service restarted without running scenario`() = testScope.runTest {
        qualityMetricsFlow.value = METRICS_EXTERNAL_ISSUE
        val repository = createRepository(createMonitor())
        runCurrent()

        assertEquals(Quality.ExternalIssue, repository.quality.value)
    }

    @Test
    fun `quality goes back to High after the grace period`() = testScope.runTest {
        qualityMetricsFlow.value = METRICS_EXTERNAL_ISSUE
        val repository = createRepository(createMonitor())
        runCurrent()

        advanceTimeBy(4.minutes)
        assertEquals(Quality.ExternalIssue, repository.quality.value)

        advanceTimeBy(1.minutes)
        runCurrent()
        assertEquals(Quality.High, repository.quality.value)
    }

    @Test
    fun `quality is not affected by metrics updates after the start`() = testScope.runTest {
        qualityMetricsFlow.value = METRICS_EXTERNAL_ISSUE
        val repository = createRepository(createMonitor())
        runCurrent()

        qualityMetricsFlow.value = METRICS_CRASHED
        runCurrent()

        assertEquals(Quality.ExternalIssue, repository.quality.value)
    }

    private companion object {
        val METRICS_FIRST_TIME = QualityMetrics(
            lastServiceStartTimeMs = INVALID_TIME,
            lastScenarioStartTimeMs = INVALID_TIME,
            accessibilityLossCount = 0,
            troubleshootingDisplayCount = 0,
        )
        val METRICS_EXTERNAL_ISSUE = METRICS_FIRST_TIME.copy(lastServiceStartTimeMs = 1_000L)
        val METRICS_CRASHED = METRICS_EXTERNAL_ISSUE.copy(lastScenarioStartTimeMs = 2_000L)
    }
}
