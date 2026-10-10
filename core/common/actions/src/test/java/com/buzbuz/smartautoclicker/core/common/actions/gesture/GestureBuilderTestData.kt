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
package com.buzbuz.smartautoclicker.core.common.actions.gesture

import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Point
import com.buzbuz.smartautoclicker.core.common.actions.gesture.builder.AndroidGestureBuilder
import com.buzbuz.smartautoclicker.core.common.actions.model.BuiltGesture
import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer
import com.buzbuz.smartautoclicker.core.common.actions.model.ResolvedFinger

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlin.math.roundToInt

/** Describes the fingers of a gesture, to be resolved by the [AndroidGestureBuilder] under test. */
internal object GestureBuilderTestData {

    /** Duration of a finger pressing and releasing without moving, as applied by the builder. */
    const val TAP_DURATION_MS = 100L
    /** Delay applied by the builder to a finger pressing down while another one is already held. */
    const val NEW_FINGER_PRESS_DELAY_MS = 1L
    /** Maximum offset applied to a randomized position, from RandomizerConfig. */
    const val RANDOMIZATION_POSITION_MAX_OFFSET_PX = 5
    /** Maximum offset applied to a randomized duration, from RandomizerConfig. */
    const val RANDOMIZATION_DURATION_MAX_OFFSET_MS = 5L

    sealed interface Motion {
        data class Press(val position: Point) : Motion
        data class Move(val position: Point, val durationMs: Long) : Motion
        data class Wait(val durationMs: Long) : Motion
        data class Release(val hold: Boolean) : Motion
    }

    class Finger(val startTimeMs: Long, val motions: List<Motion>)

    class Gesture(val fingers: List<Finger>)

    fun newGesture(vararg fingers: Finger): Gesture =
        Gesture(fingers.toList())

    fun newFinger(startTimeMs: Long = 0L, vararg motions: Motion): Finger =
        Finger(startTimeMs, motions.toList())

    fun newTapFinger(x: Int, y: Int, startTimeMs: Long = 0L): Finger =
        newFinger(startTimeMs, press(x, y), release())

    fun press(x: Int, y: Int): Motion.Press =
        Motion.Press(Point(x, y))

    fun move(x: Int, y: Int, durationMs: Long): Motion.Move =
        Motion.Move(Point(x, y), durationMs)

    fun wait(durationMs: Long): Motion.Wait =
        Motion.Wait(durationMs)

    fun release(hold: Boolean = false): Motion.Release =
        Motion.Release(hold)

    /** Resolve the fingers of a gesture with this builder, skipping the invalid ones. */
    fun AndroidGestureBuilder.resolve(gesture: Gesture): List<ResolvedFinger> =
        gesture.fingers.mapIndexedNotNull { index, finger -> resolve(index, finger) }

    fun AndroidGestureBuilder.resolve(index: Int, finger: Finger): ResolvedFinger? {
        val fingerBuilder = newFinger(index, finger.startTimeMs)
        finger.motions.forEach { motion ->
            when (motion) {
                is Motion.Press -> fingerBuilder.press(motion.position)
                is Motion.Move -> fingerBuilder.move(motion.position, motion.durationMs)
                is Motion.Wait -> fingerBuilder.wait(motion.durationMs)
                is Motion.Release -> fingerBuilder.release(motion.hold)
            }
        }
        return fingerBuilder.build()
    }

    fun AndroidGestureBuilder.build(gesture: Gesture): List<List<GestureDescription>>? =
        buildGesture(gesture)?.chains

    fun AndroidGestureBuilder.buildGesture(
        gesture: Gesture,
        heldPointer: HeldPointer? = null,
        owner: Any? = null,
    ): BuiltGesture? =
        build(resolve(gesture), "Gesture", heldPointer, owner)

    fun assertPointNear(expected: Point, actual: Point) {
        assertTrue(
            "Point $actual should be within $RANDOMIZATION_POSITION_MAX_OFFSET_PX px of $expected",
            expected.x - RANDOMIZATION_POSITION_MAX_OFFSET_PX <= actual.x
                    && actual.x <= expected.x + RANDOMIZATION_POSITION_MAX_OFFSET_PX
                    && expected.y - RANDOMIZATION_POSITION_MAX_OFFSET_PX <= actual.y
                    && actual.y <= expected.y + RANDOMIZATION_POSITION_MAX_OFFSET_PX,
        )
    }

    fun assertDurationNear(expected: Long, actual: Long) {
        assertTrue(
            "Duration $actual should be within $RANDOMIZATION_DURATION_MAX_OFFSET_MS ms of $expected",
            actual in expected - RANDOMIZATION_DURATION_MAX_OFFSET_MS..expected + RANDOMIZATION_DURATION_MAX_OFFSET_MS,
        )
    }
}
/** The points the stroke path goes through, from its start to its end. A held finger has a single point. */
internal val StrokeDescription.points: List<Point>
    get() = path.approximate(0.5f).toList().chunked(3).map { (_, x, y) -> Point(x.roundToInt(), y.roundToInt()) }

/** [StrokeDescription.getId] is hidden from the SDK, but available at runtime. */
internal val StrokeDescription.strokeId: Int
    get() = StrokeDescription::class.java.getMethod("getId").invoke(this) as Int

/** [StrokeDescription.getContinuedStrokeId] is hidden from the SDK, but available at runtime. */
internal val StrokeDescription.continuedStrokeId: Int
    get() = StrokeDescription::class.java.getMethod("getContinuedStrokeId").invoke(this) as Int

internal fun assertChainsCount(expected: Int, chains: List<List<GestureDescription>>?) {
    assertTrue("Gesture should have been built", chains != null)
    assertEquals("Invalid chains count", expected, chains!!.size)
}

internal fun assertGesturesCount(expected: Int, chain: List<GestureDescription>) {
    assertEquals("Invalid gestures count in chain", expected, chain.size)
}

internal fun assertStrokesCount(expected: Int, gesture: GestureDescription) {
    assertEquals("Invalid strokes count in gesture", expected, gesture.strokeCount)
}

internal fun assertStroke(
    stroke: StrokeDescription,
    startTimeMs: Long,
    durationMs: Long,
    from: Point,
    to: Point = from,
    willContinue: Boolean = false,
    continues: StrokeDescription? = null,
) {
    assertEquals("Invalid stroke start time", startTimeMs, stroke.startTime)
    assertEquals("Invalid stroke duration", durationMs, stroke.duration)
    assertEquals("Invalid stroke start position", from, stroke.points.first())
    assertEquals("Invalid stroke end position", to, stroke.points.last())
    assertEquals("Invalid stroke continuation flag", willContinue, stroke.willContinue())

    if (continues == null) assertFalse("Stroke should not continue another one", stroke.continuedStrokeId >= 0)
    else assertEquals("Stroke continues the wrong stroke", continues.strokeId, stroke.continuedStrokeId)
}

/** A held finger is described by a single point path: it stays at its start position without moving. */
internal fun assertHoldStroke(
    stroke: StrokeDescription,
    startTimeMs: Long,
    durationMs: Long,
    position: Point,
    willContinue: Boolean = false,
    continues: StrokeDescription? = null,
) {
    assertStroke(stroke, startTimeMs, durationMs, position, position, willContinue, continues)
    assertEquals("A held finger should not move", listOf(position), stroke.points)
}
