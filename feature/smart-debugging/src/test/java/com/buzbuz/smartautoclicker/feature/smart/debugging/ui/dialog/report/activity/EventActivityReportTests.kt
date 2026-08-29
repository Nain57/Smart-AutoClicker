/* Copyright (C) 2026 Kevin Buzeau */
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.activity

import com.buzbuz.smartautoclicker.core.smart.debugging.domain.model.report.DebugReportEventOccurrence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class EventActivityReportTests {

    @Test
    fun `duplicate names remain distinct and counts are aggregated`() {
        val report = buildEventActivityReport(
            screenEvents = listOf(screenSource(1, "Find", 0), screenSource(2, "Find", 1)),
            triggerEvents = emptyList(),
            occurrences = listOf(screenOccurrence(1), screenOccurrence(2), screenOccurrence(1)),
        )

        assertEquals(2, report.reachedEventCount)
        assertEquals(3, report.totalOccurrenceCount)
        assertEquals(listOf(2, 1), report.screenEvents.map { it.occurrenceCount })
        assertEquals(1L, report.mostFrequentEvent?.key?.eventId)
    }

    @Test
    fun `screen and trigger events with the same id never merge`() {
        val report = buildEventActivityReport(
            screenEvents = listOf(screenSource(7, "Screen", 0)),
            triggerEvents = listOf(triggerSource(7, "Trigger", 0)),
            occurrences = listOf(screenOccurrence(7), triggerOccurrence(7), triggerOccurrence(7)),
        )

        assertEquals(1, report.screenEvents.single().occurrenceCount)
        assertEquals(2, report.triggerEvents.single().occurrenceCount)
        assertEquals(EventActivityType.TRIGGER, report.mostFrequentEvent?.key?.type)
    }

    @Test
    fun `most frequent ties use scenario order deterministically`() {
        val report = buildEventActivityReport(
            screenEvents = listOf(screenSource(20, "Second", 1), screenSource(10, "First", 0)),
            triggerEvents = emptyList(),
            occurrences = listOf(screenOccurrence(20), screenOccurrence(10)),
            sort = EventActivitySort.MOST_FREQUENT,
        )

        assertEquals(listOf(10L, 20L), report.screenEvents.map { it.key.eventId })
        assertEquals(10L, report.mostFrequentEvent?.key?.eventId)
    }

    @Test
    fun `first execution puts unreached events last in scenario order`() {
        val report = buildEventActivityReport(
            screenEvents = listOf(
                screenSource(1, "Unreached first", 0),
                screenSource(2, "Reached second", 1),
                screenSource(3, "Reached first", 2),
                screenSource(4, "Unreached second", 3),
            ),
            triggerEvents = emptyList(),
            occurrences = listOf(screenOccurrence(3), screenOccurrence(2)),
            sort = EventActivitySort.FIRST_EXECUTION,
        )

        assertEquals(listOf(3L, 2L, 1L, 4L), report.screenEvents.map { it.key.eventId })
    }

    @Test
    fun `missing event references are ignored safely`() {
        val report = buildEventActivityReport(
            screenEvents = listOf(screenSource(1, "Known", 0)),
            triggerEvents = emptyList(),
            occurrences = listOf(screenOccurrence(99)),
        )

        assertEquals(0, report.reachedEventCount)
        assertEquals(0, report.totalOccurrenceCount)
        assertEquals(0, report.screenEvents.single().occurrenceCount)
        assertNull(report.mostFrequentEvent)
    }

    private fun screenSource(id: Long, name: String, order: Int) = EventActivitySource(
        key = EventActivityKey(EventActivityType.SCREEN, id),
        name = name,
        scenarioOrder = order,
    )

    private fun triggerSource(id: Long, name: String, order: Int) = EventActivitySource(
        key = EventActivityKey(EventActivityType.TRIGGER, id),
        name = name,
        scenarioOrder = order,
    )

    private fun screenOccurrence(id: Long) = DebugReportEventOccurrence.ScreenEvent(
        eventId = id,
        relativeTimestampMs = 0,
        conditionsResults = emptyList(),
        counterChanges = emptyList(),
        eventStateChanges = emptyList(),
        frameNumber = 0,
    )

    private fun triggerOccurrence(id: Long) = DebugReportEventOccurrence.TriggerEvent(
        eventId = id,
        relativeTimestampMs = 0,
        conditionsResults = emptyList(),
        counterChanges = emptyList(),
        eventStateChanges = emptyList(),
    )
}
