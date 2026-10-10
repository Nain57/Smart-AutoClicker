/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.dumb.engine

import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.common.actions.gesture.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.common.actions.gesture.ResolvedFinger
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFinger
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbGestureFingerMotion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the conversion of a [DumbAction.DumbGesture] into the fingers consumed by the [AndroidGestureBuilder]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class DumbGestureResolverTests {

    private companion object {
        val ID = Identifier(databaseId = 1L)
    }

    private fun resolve(vararg fingers: DumbGestureFinger): List<ResolvedFinger> =
        DumbAction.DumbGesture(
            id = ID,
            scenarioId = ID,
            name = "Gesture",
            repeatCount = 1,
            isRepeatInfinite = false,
            repeatDelayMs = 0L,
            fingers = fingers.toList(),
        ).toResolvedFingers(AndroidGestureBuilder.createNewBuilder(random = null))

    private fun finger(startTimeMs: Long = 0L, vararg motions: DumbGestureFingerMotion): DumbGestureFinger =
        DumbGestureFinger(ID, startTimeMs, motions.toList())

    private fun press(x: Int, y: Int) = DumbGestureFingerMotion.Press(ID, Point(x, y))
    private fun move(x: Int, y: Int, durationMs: Long) = DumbGestureFingerMotion.Move(ID, Point(x, y), durationMs)
    private fun wait(durationMs: Long) = DumbGestureFingerMotion.Wait(ID, durationMs)
    private fun release() = DumbGestureFingerMotion.Release(ID)

    @Test
    fun resolve_noFingers_returnsEmpty() {
        assertTrue(resolve().isEmpty())
    }

    @Test
    fun resolve_invalidFinger_isSkipped() {
        val fingers = resolve(finger(0L, press(10, 20)), finger(0L, press(30, 40), release()))

        assertEquals(1, fingers.size)
        assertEquals("Skipped finger should not shift the indexes", 1, fingers.single().index)
        assertEquals(Point(30, 40), fingers.single().segments.single().from)
    }

    @Test
    fun resolve_swipe_positionsAndDurationsArePassedThrough() {
        val fingers = resolve(finger(50L, press(0, 0), move(100, 0, 200L), wait(100L), move(100, 100, 300L), release()))

        val segments = fingers.single().segments
        assertEquals(3, segments.size)
        assertEquals(Point(0, 0), segments[0].from)
        assertEquals(Point(100, 0), segments[0].to)
        assertEquals(50L, segments[0].startMs)
        assertEquals(250L, segments[0].endMs)
        assertEquals(Point(100, 0), segments[1].to)
        assertEquals(350L, segments[1].endMs)
        assertEquals(Point(100, 100), segments[2].to)
        assertEquals(650L, segments[2].endMs)
    }
}
