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
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ActionMapperTests {

    @Test
    fun click_toEntity() {
        assertEquals(
            ActionTestsData.getNewClickEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewClick(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun click_toDomain() {
        assertEquals(
            ActionTestsData.getNewClick(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewClickEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun swipe_toEntity() {
        assertEquals(
            ActionTestsData.getNewSwipeEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewSwipe(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun swipe_toDomain() {
        assertEquals(
            ActionTestsData.getNewSwipe(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewSwipeEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun pause_toEntity() {
        assertEquals(
            ActionTestsData.getNewPauseEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewPause(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity(),
        )
    }

    @Test
    fun pause_toDomain() {
        assertEquals(
            ActionTestsData.getNewPause(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewPauseEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun intent_toEntity() {
        assertEquals(
            ActionTestsData.getNewIntentEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewIntent(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity()
        )
    }

    @Test
    fun intent_toDomain() {
        assertEquals(
            ActionTestsData.getNewIntent(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewIntentEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }

    @Test
    fun toggleEvent_toEntity() {
        assertEquals(
            ActionTestsData.getNewToggleEventEntity(eventId = ActionTestsData.ACTION_EVENT_ID).action,
            ActionTestsData.getNewToggleEvent(eventId = ActionTestsData.ACTION_EVENT_ID).toEntity()
        )
    }

    @Test
    fun toggleEvent_toDomain() {
        assertEquals(
            ActionTestsData.getNewToggleEvent(eventId = ActionTestsData.ACTION_EVENT_ID),
            ActionTestsData.getNewToggleEventEntity(eventId = ActionTestsData.ACTION_EVENT_ID).toDomain(),
        )
    }
}