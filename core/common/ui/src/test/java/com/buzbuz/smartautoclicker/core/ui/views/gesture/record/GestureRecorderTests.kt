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
package com.buzbuz.smartautoclicker.core.ui.views.gesture.record

import android.accessibilityservice.GestureDescription
import android.graphics.PointF
import android.os.Build
import android.os.Looper
import android.view.InputDevice
import android.view.MotionEvent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

import java.util.concurrent.TimeUnit

/**
 * Tests for [GestureRecorder].
 *
 * Uses a display density of 1 so that the dp thresholds of the finger state translate directly to pixels:
 * a finger is still within 5px, and a move direction is locked past 16px.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureRecorderTests {

    private data class Notification(val gesture: RecordedGesture?, val isFinished: Boolean)

    /** Positions of the fingers currently on screen, keyed by pointer id. Insertion order defines the pointer index. */
    private val pointers = LinkedHashMap<Int, PointF>()
    private val notifications = mutableListOf<Notification>()

    private lateinit var recorder: GestureRecorder

    @Before
    fun setUp() {
        recorder = GestureRecorder(displayDensity = 1f) { gesture, isFinished ->
            notifications += Notification(gesture, isFinished)
        }
    }

    // --- Event handling ---

    @Test
    fun processEvent_touchActions_returnsTrue() {
        assertTrue(down(id = 0, x = 10f, y = 10f, timeMs = 0L))
        assertTrue(move(id = 0, x = 100f, y = 10f, timeMs = 50L))
        assertTrue(down(id = 1, x = 200f, y = 200f, timeMs = 60L))
        assertTrue(up(id = 1, timeMs = 70L))
        assertTrue(up(id = 0, timeMs = 80L))

        assertTrue(down(id = 0, x = 10f, y = 10f, timeMs = 90L))
        assertTrue(cancel(timeMs = 100L))
    }

    @Test
    fun processEvent_unhandledAction_returnsFalseAndDoesNotNotify() {
        pointers[0] = PointF(10f, 10f)

        assertFalse(dispatch(MotionEvent.ACTION_HOVER_MOVE, actionIndex = 0, timeMs = 0L))
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun notifications_onlyTheLastOneIsFinished() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 100f, y = 10f, timeMs = 50L)
        down(id = 1, x = 200f, y = 200f, timeMs = 60L)
        up(id = 1, timeMs = 70L)
        up(id = 0, timeMs = 80L)

        assertEquals(5, notifications.size)
        assertTrue(notifications.dropLast(1).none { it.isFinished })
        assertTrue(notifications.last().isFinished)
    }

    // --- Click ---

    @Test
    fun fingerDown_notifiesUnfinishedClick() {
        down(id = 0, x = 10f, y = 20f, timeMs = 100L)

        assertEquals(listOf(Notification(click(10f, 20f, durationMs = 1L), isFinished = false)), notifications)
    }

    @Test
    fun fingerDownAndUp_notifiesFinishedClickWithPressDuration() {
        down(id = 0, x = 10f, y = 20f, timeMs = 100L)
        up(id = 0, timeMs = 250L)

        assertEquals(
            listOf(
                Notification(click(10f, 20f, durationMs = 1L), isFinished = false),
                Notification(click(10f, 20f, durationMs = 150L), isFinished = true),
            ),
            notifications,
        )
    }

    @Test
    fun fingerDownAndUp_sameMillisecond_clickDurationIsOneMs() {
        down(id = 0, x = 10f, y = 20f, timeMs = 100L)
        up(id = 0, timeMs = 100L)

        assertEquals(click(10f, 20f, durationMs = 1L), lastGesture())
    }

    @Test
    fun fingerJitterWithinStillThreshold_isStillAClickAtPressPosition() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 13f, y = 10f, timeMs = 20L)
        move(id = 0, x = 10f, y = 13f, timeMs = 40L)
        move(id = 0, x = 8f, y = 8f, timeMs = 60L)
        up(id = 0, timeMs = 80L)

        assertEquals(click(10f, 10f, durationMs = 80L), lastGesture())
    }

    @Test
    fun longPressWithJitter_isAClickWithFullDuration() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 12f, y = 10f, timeMs = 100L)
        move(id = 0, x = 11f, y = 11f, timeMs = 500L)
        move(id = 0, x = 10f, y = 12f, timeMs = 900L)
        up(id = 0, timeMs = 1000L)

        assertEquals(click(10f, 10f, durationMs = 1000L), lastGesture())
    }

    @Test
    fun secondCapture_afterFinishedOne_startsFresh() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        up(id = 0, timeMs = 100L)
        notifications.clear()

        down(id = 0, x = 50f, y = 50f, timeMs = 1000L)
        up(id = 0, timeMs = 1200L)

        assertEquals(
            listOf(
                Notification(click(50f, 50f, durationMs = 1L), isFinished = false),
                Notification(click(50f, 50f, durationMs = 200L), isFinished = true),
            ),
            notifications,
        )
    }

    // --- Single finger gesture ---

    @Test
    fun fingerMovedBeyondStillThreshold_isGestureWithSingleMove() {
        down(id = 0, x = 10f, y = 10f, timeMs = 100L)
        move(id = 0, x = 100f, y = 10f, timeMs = 150L)
        up(id = 0, timeMs = 160L)

        assertEquals(
            listOf(
                Notification(click(10f, 10f, durationMs = 1L), isFinished = false),
                Notification(
                    gesture(finger(100L, press(10f, 10f), moveTo(100f, 10f, durationMs = 50L))),
                    isFinished = false,
                ),
                Notification(
                    gesture(finger(100L, press(10f, 10f), moveTo(100f, 10f, durationMs = 50L), release())),
                    isFinished = true,
                ),
            ),
            notifications,
        )
    }

    @Test
    fun straightLineMoves_mergedIntoSingleMove() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 30f, y = 10f, timeMs = 20L)
        move(id = 0, x = 60f, y = 10f, timeMs = 40L)
        move(id = 0, x = 100f, y = 11f, timeMs = 60L)
        up(id = 0, timeMs = 70L)

        assertEquals(
            gesture(finger(0L, press(10f, 10f), moveTo(100f, 11f, durationMs = 60L), release())),
            lastGesture(),
        )
    }

    @Test
    fun movesBeforeDirectionLock_mergedIntoSingleMove() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 20f, y = 10f, timeMs = 20L)
        move(id = 0, x = 20f, y = 20f, timeMs = 40L)
        up(id = 0, timeMs = 50L)

        assertEquals(
            gesture(finger(0L, press(10f, 10f), moveTo(20f, 20f, durationMs = 40L), release())),
            lastGesture(),
        )
    }

    @Test
    fun directionChange_startsNewMove() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 100f, y = 10f, timeMs = 50L)
        move(id = 0, x = 100f, y = 100f, timeMs = 100L)
        move(id = 0, x = 100f, y = 200f, timeMs = 150L)
        up(id = 0, timeMs = 160L)

        assertEquals(
            gesture(
                finger(
                    0L,
                    press(10f, 10f),
                    moveTo(100f, 10f, durationMs = 50L),
                    moveTo(100f, 200f, durationMs = 100L),
                    release(),
                )
            ),
            lastGesture(),
        )
    }

    @Test
    fun backwardsMove_startsNewMove() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 100f, y = 10f, timeMs = 50L)
        move(id = 0, x = 50f, y = 10f, timeMs = 100L)
        up(id = 0, timeMs = 110L)

        assertEquals(
            gesture(
                finger(
                    0L,
                    press(10f, 10f),
                    moveTo(100f, 10f, durationMs = 50L),
                    moveTo(50f, 10f, durationMs = 50L),
                    release(),
                )
            ),
            lastGesture(),
        )
    }

    @Test
    fun shortPauseBetweenMoves_absorbedIntoMove() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 100f, y = 10f, timeMs = 50L)
        move(id = 0, x = 101f, y = 10f, timeMs = 100L)
        move(id = 0, x = 100f, y = 10f, timeMs = 150L)
        move(id = 0, x = 200f, y = 10f, timeMs = 200L)
        up(id = 0, timeMs = 250L)

        assertEquals(
            gesture(finger(0L, press(10f, 10f), moveTo(200f, 10f, durationMs = 200L), release())),
            lastGesture(),
        )
    }

    @Test
    fun longPauseAfterMove_recordsWaitUntilRelease() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 100f, y = 10f, timeMs = 50L)
        move(id = 0, x = 101f, y = 10f, timeMs = 100L)
        move(id = 0, x = 100f, y = 10f, timeMs = 300L)
        up(id = 0, timeMs = 400L)

        assertEquals(
            gesture(
                finger(
                    0L,
                    press(10f, 10f),
                    moveTo(100f, 10f, durationMs = 50L),
                    wait(durationMs = 350L),
                    release(),
                )
            ),
            lastGesture(),
        )
    }

    @Test
    fun moveAfterLongPause_startsNewMoveAfterWait() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 100f, y = 10f, timeMs = 50L)
        move(id = 0, x = 101f, y = 10f, timeMs = 100L)
        move(id = 0, x = 100f, y = 10f, timeMs = 300L)
        move(id = 0, x = 200f, y = 10f, timeMs = 350L)
        up(id = 0, timeMs = 400L)

        assertEquals(
            gesture(
                finger(
                    0L,
                    press(10f, 10f),
                    moveTo(100f, 10f, durationMs = 50L),
                    wait(durationMs = 250L),
                    moveTo(200f, 10f, durationMs = 50L),
                    release(),
                )
            ),
            lastGesture(),
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.N])
    fun longPause_belowOreo_absorbedIntoNextMove() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 100f, y = 10f, timeMs = 50L)
        move(id = 0, x = 101f, y = 10f, timeMs = 100L)
        move(id = 0, x = 100f, y = 10f, timeMs = 300L)
        move(id = 0, x = 200f, y = 10f, timeMs = 350L)
        up(id = 0, timeMs = 400L)

        assertEquals(
            gesture(finger(0L, press(10f, 10f), moveTo(200f, 10f, durationMs = 350L), release())),
            lastGesture(),
        )
    }

    // --- Multi fingers gesture ---

    @Test
    fun twoStationaryFingers_isGestureNotClick() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        down(id = 1, x = 200f, y = 200f, timeMs = 20L)
        up(id = 1, timeMs = 100L)
        up(id = 0, timeMs = 150L)

        assertEquals(
            listOf(
                Notification(click(10f, 10f, durationMs = 1L), isFinished = false),
                Notification(
                    gesture(finger(0L, press(10f, 10f)), finger(20L, press(200f, 200f))),
                    isFinished = false,
                ),
                Notification(
                    gesture(finger(0L, press(10f, 10f)), finger(20L, press(200f, 200f), release())),
                    isFinished = false,
                ),
                Notification(
                    gesture(finger(0L, press(10f, 10f), release()), finger(20L, press(200f, 200f), release())),
                    isFinished = true,
                ),
            ),
            notifications,
        )
    }

    @Test
    fun twoFingers_movesTrackedIndependently() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        down(id = 1, x = 200f, y = 200f, timeMs = 10L)
        move(id = 0, x = 100f, y = 10f, timeMs = 60L)
        move(id = 1, x = 200f, y = 300f, timeMs = 110L)
        up(id = 1, timeMs = 150L)
        up(id = 0, timeMs = 150L)

        assertEquals(
            gesture(
                finger(0L, press(10f, 10f), moveTo(100f, 10f, durationMs = 60L), release()),
                finger(10L, press(200f, 200f), moveTo(200f, 300f, durationMs = 100L), release()),
            ),
            lastGesture(),
        )
    }

    @Test
    fun moreThanFiveFingers_extraFingersIgnored() {
        (0 until 6).forEach { id -> down(id = id, x = id * 10f, y = 0f, timeMs = id * 10L) }
        (5 downTo 1).forEach { id -> up(id = id, timeMs = 100L) }
        up(id = 0, timeMs = 100L)

        val fingers = (lastGesture() as RecordedGesture.Gesture).fingers
        assertEquals(5, fingers.size)
        assertEquals(
            (0 until 5).map { id -> press(id * 10f, 0f) },
            fingers.map { it.motions.first() },
        )
    }

    @Test
    fun fingerReleasedAndPressedAgain_recordedAsNewFinger() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        down(id = 1, x = 200f, y = 200f, timeMs = 10L)
        up(id = 1, timeMs = 50L)
        down(id = 1, x = 300f, y = 300f, timeMs = 100L)
        up(id = 1, timeMs = 150L)
        up(id = 0, timeMs = 200L)

        assertEquals(
            gesture(
                finger(0L, press(10f, 10f), wait(durationMs = 200L), release()),
                finger(10L, press(200f, 200f), release()),
                finger(100L, press(300f, 300f), release()),
            ),
            lastGesture(),
        )
    }

    @Test
    fun fingerPressedAgain_movesOnlyAppliedToNewFinger() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        down(id = 1, x = 200f, y = 200f, timeMs = 10L)
        up(id = 1, timeMs = 50L)
        down(id = 1, x = 300f, y = 300f, timeMs = 100L)
        move(id = 1, x = 300f, y = 400f, timeMs = 150L)
        up(id = 1, timeMs = 160L)
        up(id = 0, timeMs = 200L)

        assertEquals(
            gesture(
                finger(0L, press(10f, 10f), wait(durationMs = 200L), release()),
                finger(10L, press(200f, 200f), release()),
                finger(100L, press(300f, 300f), moveTo(300f, 400f, durationMs = 50L), release()),
            ),
            lastGesture(),
        )
    }

    @Test
    fun releasedFingers_countTowardsMaxFingers() {
        down(id = 0, x = 0f, y = 0f, timeMs = 0L)
        (1..5).forEach { i ->
            down(id = 1, x = i * 10f, y = 0f, timeMs = i * 10L)
            up(id = 1, timeMs = i * 10L + 5L)
        }
        up(id = 0, timeMs = 100L)

        val fingers = (lastGesture() as RecordedGesture.Gesture).fingers
        assertEquals(5, fingers.size)
        assertEquals(
            (0 until 5).map { i -> press(i * 10f, 0f) },
            fingers.map { it.motions.first() },
        )
    }

    // --- Cancellation ---

    @Test
    fun actionCancel_dropsCaptureWithoutNotifying() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 100f, y = 10f, timeMs = 50L)
        cancel(timeMs = 60L)

        assertEquals(2, notifications.size)
        assertFalse(notifications.last().isFinished)
    }

    @Test
    fun actionCancel_nextCaptureStartsFresh() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        move(id = 0, x = 100f, y = 10f, timeMs = 50L)
        cancel(timeMs = 60L)
        notifications.clear()

        down(id = 0, x = 50f, y = 50f, timeMs = 1000L)
        up(id = 0, timeMs = 1100L)

        assertEquals(
            listOf(
                Notification(click(50f, 50f, durationMs = 1L), isFinished = false),
                Notification(click(50f, 50f, durationMs = 100L), isFinished = true),
            ),
            notifications,
        )
    }

    @Test
    fun clearCapture_dropsCaptureWithoutNotifying() {
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)
        down(id = 1, x = 200f, y = 200f, timeMs = 10L)
        recorder.clearCapture()

        assertEquals(2, notifications.size)
        notifications.clear()

        pointers.clear()
        down(id = 0, x = 50f, y = 50f, timeMs = 1000L)
        up(id = 0, timeMs = 1100L)

        assertEquals(click(50f, 50f, durationMs = 100L), lastGesture())
    }

    // --- Max duration ---

    @Test
    fun maxDuration_notSet_finishesJustBeforeAndroidMaxGestureDuration() {
        val maxDurationMs = GestureDescription.getMaxGestureDuration()
        down(id = 0, x = 10f, y = 10f, timeMs = 0L)

        idleMainLooper(maxDurationMs - 2)
        assertEquals(1, notifications.size)

        idleMainLooper(1L)
        assertEquals(Notification(click(10f, 10f, durationMs = maxDurationMs - 1), isFinished = true), notifications.last())
    }

    @Test
    fun maxDuration_reached_finishesWithReleaseAtMaxDuration() {
        recorder.maxDurationMs = 500L
        down(id = 0, x = 10f, y = 10f, timeMs = 1000L)

        idleMainLooper(499L)
        assertEquals(1, notifications.size)

        idleMainLooper(1L)
        assertEquals(
            listOf(
                Notification(click(10f, 10f, durationMs = 1L), isFinished = false),
                Notification(click(10f, 10f, durationMs = 500L), isFinished = true),
            ),
            notifications,
        )
    }

    @Test
    fun maxDuration_reached_releasesAllFingersAtMaxDuration() {
        recorder.maxDurationMs = 500L
        down(id = 0, x = 10f, y = 10f, timeMs = 1000L)
        down(id = 1, x = 200f, y = 200f, timeMs = 1100L)

        idleMainLooper(500L)

        assertEquals(
            Notification(
                gesture(
                    finger(1000L, press(10f, 10f), wait(durationMs = 500L), release()),
                    finger(1100L, press(200f, 200f), wait(durationMs = 400L), release()),
                ),
                isFinished = true,
            ),
            notifications.last(),
        )
    }

    @Test
    fun maxDuration_eventPastMaxBeforeTimeout_finishesWithReleaseAtMaxDuration() {
        recorder.maxDurationMs = 500L
        down(id = 0, x = 10f, y = 10f, timeMs = 1000L)
        move(id = 0, x = 100f, y = 10f, timeMs = 1200L)

        move(id = 0, x = 200f, y = 10f, timeMs = 1600L)

        assertEquals(3, notifications.size)
        assertEquals(
            Notification(
                gesture(finger(1000L, press(10f, 10f), moveTo(100f, 10f, durationMs = 200L), wait(300L), release())),
                isFinished = true,
            ),
            notifications.last(),
        )

        idleMainLooper(1000L)
        assertEquals("Timeout must be cancelled", 3, notifications.size)
    }

    @Test
    fun maxDuration_eventAtMax_isStillRecorded() {
        recorder.maxDurationMs = 500L
        down(id = 0, x = 10f, y = 10f, timeMs = 1000L)

        move(id = 0, x = 100f, y = 10f, timeMs = 1500L)

        assertEquals(gesture(finger(1000L, press(10f, 10f), moveTo(100f, 10f, durationMs = 500L))), lastGesture())
        assertFalse(notifications.last().isFinished)
    }

    @Test
    fun maxDuration_reached_nextCaptureStartsFresh() {
        recorder.maxDurationMs = 500L
        down(id = 0, x = 10f, y = 10f, timeMs = 1000L)
        idleMainLooper(500L)
        notifications.clear()

        pointers.clear()
        down(id = 0, x = 50f, y = 50f, timeMs = 2000L)
        up(id = 0, timeMs = 2100L)

        assertEquals(
            listOf(
                Notification(click(50f, 50f, durationMs = 1L), isFinished = false),
                Notification(click(50f, 50f, durationMs = 100L), isFinished = true),
            ),
            notifications,
        )
    }

    @Test
    fun maxDuration_fingerUpBeforeTimeout_cancelsTimeout() {
        recorder.maxDurationMs = 500L
        down(id = 0, x = 10f, y = 10f, timeMs = 1000L)
        up(id = 0, timeMs = 1100L)

        idleMainLooper(1000L)

        assertEquals(2, notifications.size)
        assertEquals(Notification(click(10f, 10f, durationMs = 100L), isFinished = true), notifications.last())
    }

    @Test
    fun maxDuration_cancelBeforeTimeout_cancelsTimeout() {
        recorder.maxDurationMs = 500L
        down(id = 0, x = 10f, y = 10f, timeMs = 1000L)
        cancel(timeMs = 1100L)

        idleMainLooper(1000L)

        assertEquals(1, notifications.size)
    }

    @Test
    fun maxDuration_clearCaptureBeforeTimeout_cancelsTimeout() {
        recorder.maxDurationMs = 500L
        down(id = 0, x = 10f, y = 10f, timeMs = 1000L)
        recorder.clearCapture()

        idleMainLooper(1000L)

        assertEquals(1, notifications.size)
    }

    // --- Motion events helpers ---

    private fun down(id: Int, x: Float, y: Float, timeMs: Long): Boolean {
        pointers[id] = PointF(x, y)
        val action = if (pointers.size == 1) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_POINTER_DOWN
        return dispatch(action, actionIndex = pointers.keys.indexOf(id), timeMs = timeMs)
    }

    private fun move(id: Int, x: Float, y: Float, timeMs: Long): Boolean {
        pointers[id] = PointF(x, y)
        return dispatch(MotionEvent.ACTION_MOVE, actionIndex = 0, timeMs = timeMs)
    }

    private fun up(id: Int, timeMs: Long): Boolean {
        val action = if (pointers.size == 1) MotionEvent.ACTION_UP else MotionEvent.ACTION_POINTER_UP
        val handled = dispatch(action, actionIndex = pointers.keys.indexOf(id), timeMs = timeMs)
        pointers.remove(id)
        return handled
    }

    private fun cancel(timeMs: Long): Boolean {
        val handled = dispatch(MotionEvent.ACTION_CANCEL, actionIndex = 0, timeMs = timeMs)
        pointers.clear()
        return handled
    }

    private fun dispatch(action: Int, actionIndex: Int, timeMs: Long): Boolean {
        val properties = pointers.keys.map { id ->
            MotionEvent.PointerProperties().apply {
                this.id = id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }
        val coords = pointers.values.map { position ->
            MotionEvent.PointerCoords().apply {
                x = position.x
                y = position.y
            }
        }

        val event = MotionEvent.obtain(
            0L,
            timeMs,
            action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
            pointers.size,
            properties.toTypedArray(),
            coords.toTypedArray(),
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0,
        )

        return try {
            recorder.processEvent(event)
        } finally {
            event.recycle()
        }
    }

    private fun idleMainLooper(durationMs: Long) =
        shadowOf(Looper.getMainLooper()).idleFor(durationMs, TimeUnit.MILLISECONDS)

    // --- Expected results helpers ---

    private fun lastGesture(): RecordedGesture? = notifications.last().gesture

    private fun click(x: Float, y: Float, durationMs: Long) =
        RecordedGesture.Click(position = PointF(x, y), durationMs = durationMs)

    private fun gesture(vararg fingers: RecordedGestureFinger) =
        RecordedGesture.Gesture(fingers = fingers.toList())

    private fun finger(pressedAtMs: Long, vararg motions: RecordedGestureFingerMotion) =
        RecordedGestureFinger(pressedAtMs = pressedAtMs, motions = motions.toList())

    private fun press(x: Float, y: Float) =
        RecordedGestureFingerMotion.Press(PointF(x, y))

    private fun moveTo(x: Float, y: Float, durationMs: Long) =
        RecordedGestureFingerMotion.Move(moveToPosition = PointF(x, y), moveToDurationMs = durationMs)

    private fun wait(durationMs: Long) =
        RecordedGestureFingerMotion.Wait(waitDurationMs = durationMs)

    private fun release() =
        RecordedGestureFingerMotion.Release
}
