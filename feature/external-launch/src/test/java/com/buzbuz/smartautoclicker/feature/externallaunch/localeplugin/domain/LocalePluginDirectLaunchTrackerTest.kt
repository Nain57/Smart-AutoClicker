/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalePluginDirectLaunchTrackerTest {

    private val tracker = LocalePluginDirectLaunchTracker()

    @Test
    fun `opening one request does not acknowledge another pending request`() {
        tracker.markPending("first")
        assertTrue(tracker.claimDirectLaunch("first"))
        tracker.markPending("second")

        assertTrue(tracker.consumeOpened("first"))
        assertFalse(tracker.consumeOpened("second"))
    }

    @Test
    fun `an acknowledgement is consumed only once`() {
        tracker.markPending("request")
        tracker.claimDirectLaunch("request")

        assertTrue(tracker.consumeOpened("request"))
        assertFalse(tracker.consumeOpened("request"))
    }

    @Test
    fun `a fallback notification acknowledgement does not remain in memory`() {
        tracker.markPending("request")
        tracker.abandon("request")
        assertFalse(tracker.claimDirectLaunch("request"))

        assertFalse(tracker.consumeOpened("request"))
    }

    @Test
    fun `a newer request supersedes an older request`() {
        tracker.markPending("first")
        tracker.markPending("second")

        assertFalse(tracker.isLatest("first"))
        assertTrue(tracker.isLatest("second"))
    }

    @Test
    fun `a late direct launch cannot revive a superseded request`() {
        tracker.markPending("first")
        tracker.markPending("second")

        assertFalse(tracker.claimDirectLaunch("first"))
        assertTrue(tracker.claimDirectLaunch("second"))
    }

    @Test
    fun `only the latest fallback request can be claimed`() {
        tracker.markPending("first")
        tracker.abandon("first")
        tracker.markPending("second")
        tracker.abandon("second")

        assertFalse(tracker.claimFallbackLaunch("first"))
        assertTrue(tracker.claimFallbackLaunch("second"))
    }

    @Test
    fun `recovered fallback claim restores process state`() {
        tracker.claimRecoveredLaunch("recovered")

        assertTrue(tracker.isLatest("recovered"))
        assertTrue(tracker.isCurrentExecution("recovered"))
    }

    @Test
    fun `a pending request makes a later request in flight`() {
        tracker.markPending("first")
        tracker.markPending("second")

        assertTrue(tracker.hasAnotherInFlightRequest("second"))

        tracker.abandon("first")
        assertFalse(tracker.hasAnotherInFlightRequest("second"))
    }

    @Test
    fun `an open execution makes a later request in flight until it closes`() {
        tracker.markPending("first")
        tracker.claimDirectLaunch("first")
        tracker.markPending("second")

        assertTrue(tracker.hasAnotherInFlightRequest("second"))

        tracker.markExecutionClosed("first")
        assertFalse(tracker.hasAnotherInFlightRequest("second"))
    }

    @Test
    fun `fallback claim does not replace an active execution`() {
        tracker.markPending("first")
        assertTrue(tracker.claimDirectLaunch("first"))
        tracker.markPending("second")
        tracker.abandon("second")

        assertFalse(tracker.claimFallbackLaunch("second"))
        assertTrue(tracker.hasAnotherInFlightRequest("second"))
        assertFalse(tracker.isCurrentExecution("second"))
    }
}
