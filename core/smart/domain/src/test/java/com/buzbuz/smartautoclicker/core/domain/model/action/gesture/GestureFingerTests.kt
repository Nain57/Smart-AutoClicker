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
package com.buzbuz.smartautoclicker.core.domain.model.action.gesture

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.gesture.GestureFingerMotion.Release.Hold
import com.buzbuz.smartautoclicker.core.domain.model.action.position.CoordinatesValue.Literal
import com.buzbuz.smartautoclicker.core.domain.model.action.position.Position

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureFingerTests {

    private fun newFinger(hold: Hold, startTimeMs: Long = 0L): GestureFinger =
        GestureFinger(
            id = Identifier(databaseId = 1L),
            startTimeMs = startTimeMs,
            motions = listOf(
                GestureFingerMotion.Press(
                    id = Identifier(databaseId = 2L),
                    position = Position.Coordinates(Literal(10), Literal(10)),
                    offset = Position.Coordinates(Literal(0), Literal(0)),
                ),
                GestureFingerMotion.Wait(id = Identifier(databaseId = 3L), durationMs = 200L),
                GestureFingerMotion.Release(id = Identifier(databaseId = 4L), hold = hold),
            ),
        )

    @Test
    fun holdForDuration_notPartOfGestureDuration() {
        assertEquals(250L, newFinger(Hold.ForDuration(durationMs = 5_000L), startTimeMs = 50L).getDurationMs())
    }

    @Test
    fun holdForDuration_longerThanGestureMaximum_isComplete() {
        val finger = newFinger(Hold.ForDuration(durationMs = Gesture.GESTURE_MAXIMUM_DURATION_MS * 2))

        assertTrue(finger.isComplete())
    }

    @Test
    fun holdForDuration_zero_isNotComplete() {
        assertFalse(newFinger(Hold.ForDuration(durationMs = 0L)).isComplete())
    }

    @Test
    fun holdForDuration_maximum_isComplete() {
        assertTrue(newFinger(Hold.ForDuration(durationMs = Hold.ForDuration.MAXIMUM_DURATION_MS)).isComplete())
    }

    @Test
    fun holdForDuration_overMaximum_isNotComplete() {
        assertFalse(newFinger(Hold.ForDuration(durationMs = Hold.ForDuration.MAXIMUM_DURATION_MS + 1)).isComplete())
    }
}
