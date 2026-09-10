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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.ConditionProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConditionPerformanceReportTests {

    @Test
    fun `profiles aggregate and percentages use the complete report denominator`() {
        val report = buildConditionPerformanceReport(
            conditions = listOf(source(1, "Shown", 0)),
            profiles = listOf(profile(1, checks = 2, fulfilled = 1, totalNs = 20), profile(99, totalNs = 80)),
        )

        assertEquals(2L, report.single().checkCount)
        assertEquals(1L, report.single().fulfilledCount)
        assertEquals(100L, report.single().totalMeasuredDurationNs)
        assertEquals("20.00%", formatPercentage(20, 100))
    }

    @Test
    fun `all-zero durations do not divide by zero and unchecked average is unavailable`() {
        val report = buildConditionPerformanceReport(
            conditions = listOf(source(1, "Skipped", 0), source(2, "Also skipped", 1)),
            profiles = emptyList(),
        )

        assertEquals(listOf(0L, 0L), report.map { it.checkCount })
        assertNull(formatAverageDuration(report.first().totalDurationNs, report.first().checkCount))
        assertEquals("0%", formatPercentage(0, 0))
    }

    @Test
    fun `tiny non-zero measurements remain visible`() {
        assertEquals("0.000000001", formatTotalDuration(1))
        assertEquals("0.000001", formatAverageDuration(totalDurationNs = 1, checkCount = 1))
        assertEquals("<0.01%", formatPercentage(1, 20_000))
    }

    @Test
    fun `durations use four meaningful digits and remove trailing decimal zeroes`() {
        assertEquals("1.234", formatTotalDuration(1_234_000_000))
        assertEquals("0.002945", formatTotalDuration(2_945_000))
        assertEquals("345.2", formatTotalDuration(345_234_000_000))
        assertEquals("3207", formatTotalDuration(3_207_000_000_000))
        assertEquals("90253", formatTotalDuration(90_253_000_000_000))
        assertEquals("0.5321", formatTotalDuration(532_100_000))
        assertEquals("424", formatTotalDuration(424_000_000_000))
        assertEquals("0.004", formatTotalDuration(4_000_000))
        assertEquals("0.9216", formatTotalDuration(921_591_721))
    }

    @Test
    fun `average milliseconds use the same four meaningful digit rule`() {
        assertEquals("1.234", formatAverageDuration(totalDurationNs = 1_234_000, checkCount = 1))
        assertEquals("0.002945", formatAverageDuration(totalDurationNs = 2_945, checkCount = 1))
        assertEquals("345.2", formatAverageDuration(totalDurationNs = 345_234_000, checkCount = 1))
        assertEquals("424", formatAverageDuration(totalDurationNs = 424_000_000, checkCount = 1))
    }

    @Test
    fun `duplicate condition names remain distinct`() {
        val report = buildConditionPerformanceReport(
            conditions = listOf(source(1, "Same", 0), source(2, "Same", 1)),
            profiles = listOf(profile(1, totalNs = 10), profile(2, totalNs = 20)),
        )

        assertEquals(listOf(2L, 1L), report.map { it.condition.id.databaseId })
    }

    @Test
    fun `total average and check sorting are descending with stable ties`() {
        val sources = listOf(source(1, "First", 0), source(2, "Second", 1), source(3, "Third", 2))
        val profiles = listOf(
            profile(1, checks = 2, totalNs = 20),
            profile(2, checks = 4, totalNs = 20),
            profile(3, checks = 1, totalNs = 5),
        )

        assertEquals(listOf(1L, 2L, 3L), ids(buildConditionPerformanceReport(sources, profiles, ConditionPerformanceSort.TOTAL_TIME)))
        assertEquals(listOf(1L, 2L, 3L), ids(buildConditionPerformanceReport(sources, profiles, ConditionPerformanceSort.AVERAGE_PER_CHECK)))
        assertEquals(listOf(2L, 1L, 3L), ids(buildConditionPerformanceReport(sources, profiles, ConditionPerformanceSort.CHECKS)))
    }

    @Test
    fun `scenario sorting restores configured order`() {
        val report = buildConditionPerformanceReport(
            conditions = listOf(source(3, "Third", 2), source(1, "First", 0), source(2, "Second", 1)),
            profiles = listOf(profile(3, totalNs = 30), profile(1, totalNs = 10), profile(2, totalNs = 20)),
            sort = ConditionPerformanceSort.SCENARIO_ORDER,
        )

        assertEquals(listOf(1L, 2L, 3L), ids(report))
    }

    @Test
    fun `short-circuited conditions stay present with zero checks and sort last`() {
        val report = buildConditionPerformanceReport(
            conditions = listOf(source(1, "Reached", 0), source(2, "Short-circuited", 1)),
            profiles = listOf(profile(1, checks = 5, totalNs = 50), profile(2, checks = 0, totalNs = 0)),
            sort = ConditionPerformanceSort.AVERAGE_PER_CHECK,
        )

        assertEquals(listOf(1L, 2L), ids(report))
        assertEquals(0L, report.last().checkCount)
    }

    @Test
    fun `empty profiling data uses zero values without hiding configured conditions`() {
        val conditions = listOf(source(1, "Condition", 0))
        val report = buildConditionPerformanceReport(conditions, emptyList())

        assertEquals(1, report.size)
        assertEquals(0L, report.single().checkCount)
        assertEquals(0L, report.single().totalDurationNs)
    }

    @Test
    fun `full and near-full percentages are not confused`() {
        assertEquals("100%", formatPercentage(100, 100))
        assertEquals("99.99%", formatPercentage(999_999, 1_000_000))
    }

    private fun source(id: Long, name: String, order: Int) = ConditionPerformanceSource(
        condition = TriggerCondition.OnTimerReached(
            id = Identifier(databaseId = id),
            eventId = Identifier(databaseId = 100 + id),
            name = name,
            durationMs = 1_000,
            restartWhenReached = false,
        ),
        eventName = "Event $id",
        scenarioOrder = order,
    )

    private fun profile(
        id: Long,
        checks: Long = 1,
        fulfilled: Long = 0,
        totalNs: Long,
    ) = ConditionProfile(
        conditionId = id,
        checkCount = checks,
        fulfilledCount = fulfilled,
        totalDurationNs = totalNs,
        minDurationNs = if (checks == 0L) 0 else totalNs / checks,
        maxDurationNs = if (checks == 0L) 0 else totalNs,
    )

    private fun ids(report: List<ConditionPerformanceEntry>): List<Long> =
        report.map { it.condition.id.databaseId }
}
