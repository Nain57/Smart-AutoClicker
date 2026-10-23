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
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests for [DumbScenario.isValid]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class DumbScenarioTests {

    private companion object {
        val ID = Identifier(databaseId = 1L)
    }

    private fun scenario(vararg actions: DumbAction, name: String = "Scenario") = DumbScenario(
        id = ID,
        name = name,
        dumbActions = actions.toList(),
        repeatCount = 1,
        isRepeatInfinite = false,
        maxDurationMin = 1,
        isDurationInfinite = true,
        randomize = false,
    )

    private fun gesture(vararg motions: DumbGestureFingerMotion) = DumbAction.DumbGesture(
        id = ID,
        scenarioId = ID,
        name = "Gesture",
        repeatCount = 1,
        isRepeatInfinite = false,
        repeatDelayMs = 0L,
        fingers = listOf(DumbGestureFinger(ID, startTimeMs = 0L, motions = motions.toList())),
    )

    private fun pause() = DumbAction.DumbPause(id = ID, scenarioId = ID, name = "Pause", pauseDurationMs = 100L)

    private fun press() = DumbGestureFingerMotion.Press(ID, Point(10, 10))
    private fun move(durationMs: Long) = DumbGestureFingerMotion.Move(ID, Point(100, 10), durationMs)
    private fun release() = DumbGestureFingerMotion.Release(ID)

    @Test
    fun isValid_validActions_isValid() {
        assertTrue(scenario(pause(), gesture(press(), move(durationMs = 100L), release())).isValid())
    }

    @Test
    fun isValid_noActions_isInvalid() {
        assertFalse(scenario().isValid())
    }

    @Test
    fun isValid_emptyName_isInvalid() {
        assertFalse(scenario(pause(), name = "").isValid())
    }

    @Test
    fun isValid_oneInvalidGesture_isInvalid() {
        assertFalse(scenario(pause(), gesture(press(), move(durationMs = 0L), release())).isValid())
    }
}
