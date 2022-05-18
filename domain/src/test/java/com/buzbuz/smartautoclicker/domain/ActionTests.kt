/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.domain

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.domain.utils.TestsData

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [Action] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ActionTests {

    @Test
    fun click_toEntity() {
        assertEquals(
            TestsData.getNewClickEntity(priority = 0, eventId = TestsData.EVENT_ID),
            TestsData.getNewClick(eventId = TestsData.EVENT_ID).toEntity()
        )
    }

    @Test
    fun click_toDomain() {
        assertEquals(
            TestsData.getNewClick(eventId = TestsData.EVENT_ID),
            TestsData.getNewClickEntity(priority = 0, eventId = TestsData.EVENT_ID).toAction(),
        )
    }

    @Test
    fun click_getIdentifier() {
        assertEquals(
            TestsData.CLICK_ID,
            TestsData.getNewClick(eventId = TestsData.EVENT_ID).id,
        )
    }

    @Test
    fun click_isComplete() {
        assertTrue(TestsData.getNewClick(eventId = TestsData.EVENT_ID).isComplete())
    }

    @Test
    fun click_isComplete_incomplete() {
        assertFalse(TestsData.getNewClick(eventId = TestsData.EVENT_ID, name = null, x = null, y = null).isComplete())
    }

    @Test
    fun click_cleanupIds() {
        val click = TestsData.getNewClick(eventId = TestsData.EVENT_ID)
        click.cleanUpIds()

        assertEquals("Action id isn't cleaned", 0L, click.id)
        assertEquals("Event id isn't cleaned", 0L, click.eventId)
    }

    @Test
    fun click_deepCopy() {
        val click = TestsData.getNewClick(eventId = TestsData.EVENT_ID)
        assertEquals(click, click.deepCopy())
    }

    @Test
    fun swipe_toEntity() {
        assertEquals(
            TestsData.getNewSwipeEntity(priority = 0, eventId = TestsData.EVENT_ID),
            TestsData.getNewSwipe(eventId = TestsData.EVENT_ID).toEntity()
        )
    }

    @Test
    fun swipe_toDomain() {
        assertEquals(
            TestsData.getNewSwipe(eventId = TestsData.EVENT_ID),
            TestsData.getNewSwipeEntity(priority = 0, eventId = TestsData.EVENT_ID).toAction(),
        )
    }

    @Test
    fun swipe_getIdentifier() {
        assertEquals(
            TestsData.SWIPE_ID,
            TestsData.getNewSwipe(eventId = TestsData.EVENT_ID).id,
        )
    }

    @Test
    fun swipe_isComplete() {
        assertTrue(TestsData.getNewSwipe(eventId = TestsData.EVENT_ID).isComplete())
    }

    @Test
    fun swipe_isComplete_incomplete() {
        assertFalse(TestsData.getNewSwipe(eventId = TestsData.EVENT_ID, name = null, fromX = null, fromY = null,
            toX = null, toY = null).isComplete())
    }

    @Test
    fun swipe_cleanupIds() {
        val swipe = TestsData.getNewSwipe(eventId = TestsData.EVENT_ID)
        swipe.cleanUpIds()

        assertEquals("Action id isn't cleaned", 0L, swipe.id)
        assertEquals("Event id isn't cleaned", 0L, swipe.eventId)
    }

    @Test
    fun swipe_deepCopy() {
        val swipe = TestsData.getNewSwipe(eventId = TestsData.EVENT_ID)
        assertEquals(swipe, swipe.deepCopy())
    }

    @Test
    fun pause_toEntity() {
        assertEquals(
            TestsData.getNewPauseEntity(priority = 0, eventId = TestsData.EVENT_ID),
            TestsData.getNewPause(eventId = TestsData.EVENT_ID).toEntity()
        )
    }

    @Test
    fun pause_toDomain() {
        assertEquals(
            TestsData.getNewPause(eventId = TestsData.EVENT_ID),
            TestsData.getNewPauseEntity(priority = 0, eventId = TestsData.EVENT_ID).toAction(),
        )
    }

    @Test
    fun pause_getIdentifier() {
        assertEquals(
            TestsData.PAUSE_ID,
            TestsData.getNewPause(eventId = TestsData.EVENT_ID).id,
        )
    }

    @Test
    fun pause_isComplete() {
        assertTrue(TestsData.getNewPause(eventId = TestsData.EVENT_ID).isComplete())
    }

    @Test
    fun pause_isComplete_incomplete() {
        assertFalse(TestsData.getNewPause(eventId = TestsData.EVENT_ID, name = null).isComplete())
    }

    @Test
    fun pause_cleanupIds() {
        val pause = TestsData.getNewPause(eventId = TestsData.EVENT_ID)
        pause.cleanUpIds()

        assertEquals("Action id isn't cleaned", 0L, pause.id)
        assertEquals("Event id isn't cleaned", 0L, pause.eventId)
    }

    @Test
    fun pause_deepCopy() {
        val pause = TestsData.getNewPause(eventId = TestsData.EVENT_ID)
        assertEquals(pause, pause.deepCopy())
    }

    @Test
    fun intent_toEntity() {
        assertEquals(
            TestsData.getNewIntentEntity(priority = 0, eventId = TestsData.EVENT_ID),
            TestsData.getNewIntent(eventId = TestsData.EVENT_ID).toEntity()
        )
    }

    @Test
    fun intent_toDomain() {
        assertEquals(
            TestsData.getNewIntent(eventId = TestsData.EVENT_ID),
            TestsData.getNewIntentEntity(priority = 0, eventId = TestsData.EVENT_ID).toAction(),
        )
    }

    @Test
    fun intent_getIdentifier() {
        assertEquals(
            TestsData.INTENT_ID,
            TestsData.getNewIntent(eventId = TestsData.EVENT_ID).id,
        )
    }

    @Test
    fun intent_isComplete() {
        assertTrue(TestsData.getNewIntent(eventId = TestsData.EVENT_ID).isComplete())
    }

    @Test
    fun intent_isComplete_incomplete() {
        assertFalse(TestsData.getNewIntent(eventId = TestsData.EVENT_ID, name = null).isComplete())
    }

    @Test
    fun intent_cleanupIds() {
        val intent = TestsData.getNewIntent(
            eventId = TestsData.EVENT_ID,
            intentExtras = mutableListOf(TestsData.getNewIntentExtra(value = 20))
        )
        intent.cleanUpIds()

        assertEquals("Action id isn't cleaned", 0L, intent.id)
        assertEquals("Event id isn't cleaned", 0L, intent.eventId)
        assertEquals("Intent extra id isn't cleaned", 0L, intent.extras!![0].id)
        assertEquals("Intent extra action id isn't cleaned", 0L, intent.extras!![0].actionId)
    }

    @Test
    fun intent_deepCopy() {
        val intent = TestsData.getNewIntent(eventId = TestsData.EVENT_ID)
        assertEquals(intent, intent.deepCopy())
    }
}