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

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.graphics.Path
import android.graphics.Point
import android.os.Build

import com.buzbuz.smartautoclicker.core.common.actions.model.BuiltGesture
import com.buzbuz.smartautoclicker.core.common.actions.model.HeldPointer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when` as mockWhen
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureExecutorTests {

    private lateinit var service: AccessibilityService
    private lateinit var userTouchWatcher: UserTouchWatcher
    private lateinit var heldPointerManager: HeldPointerManager
    private lateinit var executor: GestureExecutor
    private lateinit var callbackCaptor: ArgumentCaptor<GestureResultCallback>

    @Before
    fun setUp() {
        service = mock(AccessibilityService::class.java)
        userTouchWatcher = mock(UserTouchWatcher::class.java)
        heldPointerManager = HeldPointerManager(userTouchWatcher)
        executor = GestureExecutor(heldPointerManager, userTouchWatcher)
        callbackCaptor = ArgumentCaptor.forClass(GestureResultCallback::class.java)
    }

    @Test
    fun dispatchBuiltGesture_completedCallback_returnsTrue() = runTest {
        val result = async { executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()))) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())

        callbackCaptor.value.onCompleted(null)

        assertTrue(result.await())
    }

    @Test
    fun dispatchBuiltGesture_completed_recordsHeldPointer() = runTest {
        val pointer = heldPointer()

        val result = async { executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()), held = pointer)) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())
        callbackCaptor.value.onCompleted(null)

        assertTrue(result.await())
        assertEquals(pointer, executor.getHeldPointer())
    }

    @Test
    fun dispatchBuiltGesture_completedWithoutHeldPointer_clearsPreviousHeldPointer() = runTest {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())

        val result = async { executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()))) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())
        callbackCaptor.value.onCompleted(null)

        assertTrue(result.await())
        assertNull(executor.getHeldPointer())
    }

    @Test
    fun dispatchBuiltGesture_missingCallback_timesOut_andReturnsFalse() = runTest {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())

        val result = async {
            executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()), held = heldPointer()))
        }
        runCurrent()
        advanceTimeBy(200.milliseconds)

        assertFalse(result.await())
        assertNull("A timed out gesture tears down the held pointer", executor.getHeldPointer())
    }

    @Test
    fun dispatchBuiltGesture_dispatchException_returnsFalse() = runTest {
        doThrow(IllegalStateException("Accessibility service is unavailable"))
            .`when`(service)
            .dispatchGesture(any(), any(), any())

        assertFalse(executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()))))
    }

    @Test
    fun dispatchBuiltGesture_lateCallbackAfterTimeout_doesNotCompleteNextGesture() = runTest {
        val timedOutResult = async { executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()))) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())
        val timedOutCallback = callbackCaptor.value

        advanceTimeBy(200.milliseconds)
        assertFalse(timedOutResult.await())

        val nextResult = async { executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()))) }
        runCurrent()
        verify(service, times(2)).dispatchGesture(any(), callbackCaptor.capture(), any())

        timedOutCallback.onCompleted(null)
        runCurrent()
        assertFalse(nextResult.isCompleted)

        callbackCaptor.allValues.last().onCompleted(null)

        assertTrue(nextResult.await())
    }

    @Test
    fun dispatchBuiltGesture_dispatchesWholeChainBeforeAwaitingResults() = runTest {
        val result = async {
            executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture(), gesture(), gesture())))
        }
        runCurrent()

        // The whole chain must be handed to the system in one go: waiting for a result in between would freeze the
        // fingers on the screen until the next gesture is scheduled.
        verify(service, times(3)).dispatchGesture(any(), callbackCaptor.capture(), any())
        assertFalse(result.isCompleted)

        callbackCaptor.allValues.forEach { callback -> callback.onCompleted(null) }

        assertTrue(result.await())
    }

    @Test
    fun dispatchBuiltGesture_severalChains_awaitsEachChainBeforeTheNext() = runTest {
        val result = async {
            executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()), listOf(gesture(), gesture())))
        }
        runCurrent()
        verify(service, times(1)).dispatchGesture(any(), callbackCaptor.capture(), any())

        callbackCaptor.value.onCompleted(null)
        runCurrent()
        verify(service, times(3)).dispatchGesture(any(), callbackCaptor.capture(), any())
        assertFalse(result.isCompleted)

        callbackCaptor.allValues.takeLast(2).forEach { callback -> callback.onCompleted(null) }

        assertTrue(result.await())
    }

    @Test
    fun dispatchBuiltGesture_oneCancelledCallback_returnsFalse() = runTest {
        val result = async { executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture(), gesture()))) }
        runCurrent()
        verify(service, times(2)).dispatchGesture(any(), callbackCaptor.capture(), any())

        callbackCaptor.allValues.first().onCompleted(null)
        callbackCaptor.allValues.last().onCancelled(null)

        assertFalse(result.await())
    }

    @Test
    fun dispatchBuiltGesture_cancelledChain_skipsNextChains_andDropsHeldPointer() = runTest {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())

        val result = async {
            val twoChains = builtGesture(listOf(gesture()), listOf(gesture()), held = heldPointer())
            executor.dispatchBuiltGesture(service, twoChains)
        }
        runCurrent()
        verify(service, times(1)).dispatchGesture(any(), callbackCaptor.capture(), any())

        callbackCaptor.value.onCancelled(null)

        assertFalse(result.await())
        verify(service, times(1)).dispatchGesture(any(), any(), any())
        assertNull(executor.getHeldPointer())
    }

    @Test
    fun dispatchBuiltGesture_dispatchException_skipsRemainingGestures() = runTest {
        doThrow(IllegalStateException("Accessibility service is unavailable"))
            .`when`(service)
            .dispatchGesture(any(), any(), any())

        val result = executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture(), gesture(), gesture())))

        // Once the chain is broken, the remaining gestures would continue pointers that are no longer down.
        assertFalse(result)
        verify(service, times(1)).dispatchGesture(any(), any(), any())
    }

    @Test
    fun dispatchBuiltGesture_leavingPointerHeld_watchesUserTouchesBeforeDispatching() = runTest {
        val result = async {
            executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()), held = heldPointer()))
        }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())
        callbackCaptor.value.onCompleted(null)

        assertTrue(result.await())
        // A user touch interrupting the gesture leaving the pointer down must be noticed too
        inOrder(userTouchWatcher, service).apply {
            verify(userTouchWatcher).start(service)
            verify(service).dispatchGesture(any(), any(), any())
        }
        verify(userTouchWatcher, never()).stop()
    }

    @Test
    fun dispatchBuiltGesture_withoutHeldPointer_doesNotWatchUserTouches() = runTest {
        val result = async { executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()))) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())
        callbackCaptor.value.onCompleted(null)

        assertTrue(result.await())
        verify(userTouchWatcher, never()).start(service)
    }

    @Test
    fun dispatchBuiltGesture_releasingHeldPointer_stopsWatchingUserTouches() = runTest {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())

        val result = async { executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()))) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())
        callbackCaptor.value.onCompleted(null)

        assertTrue(result.await())
        verify(userTouchWatcher).stop()
    }

    @Test
    fun dispatchBuiltGesture_cancelled_stopsWatchingUserTouches() = runTest {
        val result = async {
            executor.dispatchBuiltGesture(service, builtGesture(listOf(gesture()), held = heldPointer()))
        }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())
        callbackCaptor.value.onCancelled(null)

        assertFalse(result.await())
        verify(userTouchWatcher).stop()
    }

    @Test
    fun checkHeldPointer_nothingHeld_returnsTrue() {
        assertTrue(executor.checkHeldPointer())
        verifyNoInteractions(service)
    }

    @Test
    fun checkHeldPointer_noUserTouch_returnsTrue_andKeepsPointer() {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())

        assertTrue(executor.checkHeldPointer())
        verifyNoInteractions(service)
        verify(userTouchWatcher, never()).stop()
        assertEquals(OWNER, executor.getHeldPointer()?.owner)
    }

    @Test
    fun checkHeldPointer_userTouched_returnsFalse_dropsPointer_andKeepsWatching() {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())
        mockWhen(userTouchWatcher.hasUserTouchedSince(anyLong())).thenReturn(true)

        assertFalse(executor.checkHeldPointer())
        verifyNoInteractions(service)
        // The event is often held again right away, the window must not be removed and added back meanwhile
        verify(userTouchWatcher, never()).stop()
        assertNull(executor.getHeldPointer())
    }

    @Test
    fun releaseHeldPointer_held_stopsWatchingUserTouches() = runTest {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())

        executor.releaseHeldPointer(null)

        verify(userTouchWatcher).stop()
    }

    @Test
    fun releaseHeldPointer_nothingHeld_returnsTrueWithoutDispatching() = runTest {
        assertTrue(executor.releaseHeldPointer(service))
        verifyNoInteractions(service)
    }

    @Test
    fun releaseHeldPointer_held_dispatchesRelease_andDropsPointer() = runTest {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())

        val result = async { executor.releaseHeldPointer(service) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())
        callbackCaptor.value.onCompleted(null)

        assertTrue(result.await())
        assertNull(executor.getHeldPointer())
    }

    @Test
    fun releaseHeldPointer_noService_returnsFalse_andDropsPointer() = runTest {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())

        assertFalse(executor.releaseHeldPointer(null))
        assertNull(executor.getHeldPointer())
    }

    @Test
    fun clear_releasesHeldPointerWithoutWaiting() {
        heldPointerManager.onHeldPointerContinuationChanged(heldPointer())

        executor.clear(service)

        verify(service).dispatchGesture(any(), isNull(), isNull())
        verify(userTouchWatcher).stop()
        assertNull(executor.getHeldPointer())
    }

    @Test
    fun dispatchPixelWorkaroundGestureIfNeeded_tooCloseToCreation_nothingDispatched() = runTest {
        executor.dispatchPixelWorkaroundGestureIfNeeded(service)

        verify(service, never()).dispatchGesture(any(), any(), any())
    }

    private fun builtGesture(vararg chains: List<GestureDescription>, held: HeldPointer? = null): BuiltGesture =
        BuiltGesture(chains.toList(), held)

    private fun heldPointer(): HeldPointer =
        HeldPointer(
            stroke = StrokeDescription(
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(HELD_POSITION.x.toFloat(), HELD_POSITION.y.toFloat())
                },
                0L,
                100L,
                true,
            ),
            position = HELD_POSITION,
            owner = OWNER,
        )

    private fun gesture(): GestureDescription = GestureDescription.Builder()
        .addStroke(
            StrokeDescription(
                Path().apply {
                    moveTo(0f, 0f)
                    lineTo(1f, 1f)
                },
                0L,
                100L,
            )
        )
        .build()
}

private val HELD_POSITION = Point(10, 20)
private const val OWNER = "owner"
