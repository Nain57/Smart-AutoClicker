/* Copyright (C) 2026 Kevin Buzeau */
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.conditions

import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.ConditionProfile
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

enum class ConditionPerformanceSort {
    TOTAL_TIME,
    AVERAGE_PER_CHECK,
    CHECKS,
    SCENARIO_ORDER,
}

data class ConditionPerformanceSource(
    val condition: Condition,
    val eventName: String,
    val scenarioOrder: Int,
)

data class ConditionPerformanceEntry(
    val condition: Condition,
    val eventName: String,
    val checkCount: Long,
    val fulfilledCount: Long,
    val totalDurationNs: Long,
    val minDurationNs: Long,
    val maxDurationNs: Long,
    val totalMeasuredDurationNs: Long,
    val scenarioOrder: Int,
) {
    val averageDurationNs: BigDecimal?
        get() = if (checkCount == 0L) null
        else BigDecimal.valueOf(totalDurationNs).divide(BigDecimal.valueOf(checkCount), 12, RoundingMode.HALF_UP)
}

internal fun buildConditionPerformanceReport(
    conditions: List<ConditionPerformanceSource>,
    profiles: List<ConditionProfile>,
    sort: ConditionPerformanceSort = ConditionPerformanceSort.TOTAL_TIME,
): List<ConditionPerformanceEntry> {
    val aggregatedProfiles = profiles
        .groupBy(ConditionProfile::conditionId)
        .mapValues { (_, entries) -> entries.aggregate() }
    val totalMeasuredDurationNs = aggregatedProfiles.values.sumOf(ConditionProfile::totalDurationNs)

    return conditions.map { source ->
        val profile = aggregatedProfiles[source.condition.id.databaseId]
        ConditionPerformanceEntry(
            condition = source.condition,
            eventName = source.eventName,
            checkCount = profile?.checkCount ?: 0L,
            fulfilledCount = profile?.fulfilledCount ?: 0L,
            totalDurationNs = profile?.totalDurationNs ?: 0L,
            minDurationNs = profile?.minDurationNs ?: 0L,
            maxDurationNs = profile?.maxDurationNs ?: 0L,
            totalMeasuredDurationNs = totalMeasuredDurationNs,
            scenarioOrder = source.scenarioOrder,
        )
    }.sortedWith(sort.comparator())
}

internal fun buildConditionPerformanceReportOrNull(
    conditions: List<ConditionPerformanceSource>,
    profiles: List<ConditionProfile>?,
    sort: ConditionPerformanceSort = ConditionPerformanceSort.TOTAL_TIME,
): List<ConditionPerformanceEntry>? = profiles?.let { buildConditionPerformanceReport(conditions, it, sort) }

internal fun List<ScreenEvent>.toScreenConditionPerformanceSources(): List<ConditionPerformanceSource> =
    sortedBy(ScreenEvent::priority).flatMap { event ->
        event.conditions.sortedBy { condition -> condition.priority }.map { condition ->
            ConditionPerformanceSource(condition, event.name, scenarioOrder = 0)
        }
    }.mapIndexed { index, source -> source.copy(scenarioOrder = index) }

internal fun List<TriggerEvent>.toTriggerConditionPerformanceSources(startingOrder: Int): List<ConditionPerformanceSource> =
    flatMap { event ->
        event.conditions.map { condition -> ConditionPerformanceSource(condition, event.name, scenarioOrder = 0) }
    }.mapIndexed { index, source -> source.copy(scenarioOrder = startingOrder + index) }

private fun List<ConditionProfile>.aggregate(): ConditionProfile =
    ConditionProfile(
        conditionId = first().conditionId,
        checkCount = sumOf(ConditionProfile::checkCount),
        fulfilledCount = sumOf(ConditionProfile::fulfilledCount),
        totalDurationNs = sumOf(ConditionProfile::totalDurationNs),
        minDurationNs = filter { it.checkCount > 0 }.minOfOrNull(ConditionProfile::minDurationNs) ?: 0L,
        maxDurationNs = maxOfOrNull(ConditionProfile::maxDurationNs) ?: 0L,
    )

private fun ConditionPerformanceSort.comparator(): Comparator<ConditionPerformanceEntry> =
    Comparator { left, right ->
        val comparison = when (this) {
            ConditionPerformanceSort.TOTAL_TIME -> right.totalDurationNs.compareTo(left.totalDurationNs)
            ConditionPerformanceSort.CHECKS -> right.checkCount.compareTo(left.checkCount)
            ConditionPerformanceSort.AVERAGE_PER_CHECK -> compareAverageDescending(left, right)
            ConditionPerformanceSort.SCENARIO_ORDER -> left.scenarioOrder.compareTo(right.scenarioOrder)
        }
        if (comparison != 0) comparison else left.scenarioOrder.compareTo(right.scenarioOrder)
    }

private fun compareAverageDescending(
    left: ConditionPerformanceEntry,
    right: ConditionPerformanceEntry,
): Int {
    if (left.checkCount == 0L) return if (right.checkCount == 0L) 0 else 1
    if (right.checkCount == 0L) return -1

    val leftRatio = BigInteger.valueOf(left.totalDurationNs).multiply(BigInteger.valueOf(right.checkCount))
    val rightRatio = BigInteger.valueOf(right.totalDurationNs).multiply(BigInteger.valueOf(left.checkCount))
    return rightRatio.compareTo(leftRatio)
}

internal fun formatTotalDuration(totalDurationNs: Long): String =
    formatWithFourSignificantDigits(BigDecimal.valueOf(totalDurationNs, 9))

internal fun formatAverageDuration(totalDurationNs: Long, checkCount: Long): String? {
    if (checkCount == 0L) return null
    val averageMs = BigDecimal.valueOf(totalDurationNs)
        .divide(BigDecimal.valueOf(checkCount), 6, RoundingMode.HALF_UP)
        .movePointLeft(6)
    return formatWithFourSignificantDigits(averageMs)
}

internal fun formatPercentage(totalDurationNs: Long, totalMeasuredDurationNs: Long): String {
    if (totalDurationNs == 0L || totalMeasuredDurationNs == 0L) return "0%"
    if (totalDurationNs == totalMeasuredDurationNs) return "100%"

    val percentage = BigDecimal.valueOf(totalDurationNs)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(totalMeasuredDurationNs), 8, RoundingMode.HALF_UP)
    if (percentage < BigDecimal("0.01")) return "<0.01%"

    val rounded = percentage.setScale(2, RoundingMode.HALF_UP).min(BigDecimal("99.99"))
    return "${rounded.toPlainString()}%"
}

internal fun formatCount(value: Long, locale: Locale = Locale.getDefault()): String =
    NumberFormat.getIntegerInstance(locale).format(value)

private fun formatWithFourSignificantDigits(value: BigDecimal): String {
    if (value.signum() == 0) return "0"

    val normalized = value.abs().stripTrailingZeros()
    val digitsBeforeDecimal = normalized.precision() - normalized.scale()
    val decimalPlaces = (SIGNIFICANT_DIGITS - digitsBeforeDecimal).coerceAtLeast(0)
    val rounded = value.setScale(decimalPlaces, RoundingMode.HALF_UP).stripTrailingZeros()
    return rounded.toPlainString()
}

private const val SIGNIFICANT_DIGITS = 4
