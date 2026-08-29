/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity

import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence


enum class EventActivityType {
    SCREEN,
    TRIGGER,
}

enum class EventActivitySort {
    SCENARIO_ORDER,
    MOST_FREQUENT,
    FIRST_EXECUTION,
}

data class EventActivityKey(
    val type: EventActivityType,
    val eventId: Long,
)

data class EventActivitySource(
    val key: EventActivityKey,
    val name: String,
    val scenarioOrder: Int,
)

data class EventActivityEntry(
    val key: EventActivityKey,
    val name: String,
    val scenarioOrder: Int,
    val occurrenceCount: Int,
    val firstOccurrenceIndex: Int?,
)

data class EventActivityReport(
    val reachedEventCount: Int,
    val totalOccurrenceCount: Int,
    val mostFrequentEvent: EventActivityEntry?,
    val screenEvents: List<EventActivityEntry>,
    val triggerEvents: List<EventActivityEntry>,
)

internal fun buildEventActivityReport(
    screenEvents: List<EventActivitySource>,
    triggerEvents: List<EventActivitySource>,
    occurrences: List<DebugReportEventOccurrence>,
    sort: EventActivitySort = EventActivitySort.SCENARIO_ORDER,
): EventActivityReport {
    val sourcesByKey = (screenEvents + triggerEvents).associateBy(EventActivitySource::key)
    val occurrenceCounts = mutableMapOf<EventActivityKey, Int>()
    val firstOccurrenceIndexes = mutableMapOf<EventActivityKey, Int>()

    occurrences.forEachIndexed { index, occurrence ->
        val key = occurrence.toEventActivityKey()
        if (key !in sourcesByKey) return@forEachIndexed

        occurrenceCounts[key] = occurrenceCounts.getOrDefault(key, 0) + 1
        firstOccurrenceIndexes.putIfAbsent(key, index)
    }

    val allEntries = sourcesByKey.values.map { source ->
        EventActivityEntry(
            key = source.key,
            name = source.name,
            scenarioOrder = source.scenarioOrder,
            occurrenceCount = occurrenceCounts.getOrDefault(source.key, 0),
            firstOccurrenceIndex = firstOccurrenceIndexes[source.key],
        )
    }
    val reachedEntries = allEntries.filter { entry -> entry.occurrenceCount > 0 }

    return EventActivityReport(
        reachedEventCount = reachedEntries.size,
        totalOccurrenceCount = reachedEntries.sumOf(EventActivityEntry::occurrenceCount),
        mostFrequentEvent = reachedEntries.sortedWith(mostFrequentComparator).firstOrNull(),
        screenEvents = allEntries
            .filter { entry -> entry.key.type == EventActivityType.SCREEN }
            .sortedWith(sort.comparator()),
        triggerEvents = allEntries
            .filter { entry -> entry.key.type == EventActivityType.TRIGGER }
            .sortedWith(sort.comparator()),
    )
}

internal fun List<ScreenEvent>.toScreenEventActivitySources(): List<EventActivitySource> =
    mapIndexed { index, event ->
        EventActivitySource(
            key = EventActivityKey(EventActivityType.SCREEN, event.id.databaseId),
            name = event.name,
            scenarioOrder = index,
        )
    }

internal fun List<TriggerEvent>.toTriggerEventActivitySources(): List<EventActivitySource> =
    mapIndexed { index, event ->
        EventActivitySource(
            key = EventActivityKey(EventActivityType.TRIGGER, event.id.databaseId),
            name = event.name,
            scenarioOrder = index,
        )
    }

private fun DebugReportEventOccurrence.toEventActivityKey(): EventActivityKey =
    when (this) {
        is DebugReportEventOccurrence.ScreenEvent -> EventActivityKey(EventActivityType.SCREEN, eventId)
        is DebugReportEventOccurrence.TriggerEvent -> EventActivityKey(EventActivityType.TRIGGER, eventId)
    }

private val scenarioOrderComparator: Comparator<EventActivityEntry> =
    compareBy<EventActivityEntry> { entry -> entry.scenarioOrder }
        .thenBy { entry -> entry.key.eventId }

private val mostFrequentComparator: Comparator<EventActivityEntry> =
    compareByDescending<EventActivityEntry> { entry -> entry.occurrenceCount }
        .thenBy { entry -> entry.key.type.ordinal }
        .then(scenarioOrderComparator)

private val firstExecutionComparator: Comparator<EventActivityEntry> =
    compareBy<EventActivityEntry> { entry -> entry.firstOccurrenceIndex ?: Int.MAX_VALUE }
        .then(scenarioOrderComparator)

private fun EventActivitySort.comparator(): Comparator<EventActivityEntry> =
    when (this) {
        EventActivitySort.SCENARIO_ORDER -> scenarioOrderComparator
        EventActivitySort.MOST_FREQUENT -> mostFrequentComparator
        EventActivitySort.FIRST_EXECUTION -> firstExecutionComparator
    }
