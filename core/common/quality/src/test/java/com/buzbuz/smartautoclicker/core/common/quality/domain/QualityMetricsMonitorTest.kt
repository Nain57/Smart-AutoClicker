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

import com.buzbuz.smartautoclicker.core.common.quality.data.INVALID_TIME
import com.buzbuz.smartautoclicker.core.common.quality.data.QualityDataSource
import com.buzbuz.smartautoclicker.core.common.quality.data.QualityMetrics

import io.mockk.every
import io.mockk.mockk

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QualityMetricsMonitorTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val mockQualityDataSource: QualityDataSource = mockk()
    private val qualityMetricsFlow = MutableStateFlow(METRICS_INITIAL)

    @Before
    fun setUp() {
        every { mockQualityDataSource.qualityMetrics() } returns qualityMetricsFlow
    }

    @Test
    fun `startingQualityMetrics is replayed to subscribers arriving after the first read`() = testScope.runTest {
        val monitor = QualityMetricsMonitor(mockQualityDataSource, testDispatcher)
        // Let the eager sharing consume the data source before anyone subscribes.
        advanceUntilIdle()

        val starting = withTimeoutOrNull(TIMEOUT_MS) { monitor.startingQualityMetrics.first() }

        assertEquals(METRICS_INITIAL, starting)
    }

    @Test
    fun `startingQualityMetrics keeps the first value when metrics are updated`() = testScope.runTest {
        val monitor = QualityMetricsMonitor(mockQualityDataSource, testDispatcher)
        advanceUntilIdle()

        qualityMetricsFlow.value = METRICS_UPDATED
        advanceUntilIdle()

        assertEquals(METRICS_INITIAL, monitor.startingQualityMetrics.first())
    }

    private companion object {
        const val TIMEOUT_MS = 1_000L

        val METRICS_INITIAL = QualityMetrics(
            lastServiceStartTimeMs = 1_000L,
            lastScenarioStartTimeMs = INVALID_TIME,
            accessibilityLossCount = 1,
            troubleshootingDisplayCount = 0,
        )
        val METRICS_UPDATED = METRICS_INITIAL.copy(lastServiceStartTimeMs = 2_000L)
    }
}
