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
import android.graphics.Path
import android.os.Build

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class GestureExecutorTests {

    @Test
    fun dispatchGesture_completedCallback_returnsTrue() = runTest {
        val service = mock(AccessibilityService::class.java)
        val executor = GestureExecutor()
        val callbackCaptor = ArgumentCaptor.forClass(GestureResultCallback::class.java)

        val result = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())

        callbackCaptor.value.onCompleted(null)

        assertTrue(result.await())
    }

    @Test
    fun dispatchGesture_missingCallback_timesOut_andReturnsFalse() = runTest {
        val service = mock(AccessibilityService::class.java)
        val executor = GestureExecutor()

        val result = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        advanceTimeBy(200.milliseconds)

        assertFalse(result.await())
    }

    @Test
    fun dispatchGesture_dispatchException_returnsFalse() = runTest {
        val service = mock(AccessibilityService::class.java)
        doThrow(IllegalStateException("Accessibility service is unavailable"))
            .`when`(service)
            .dispatchGesture(any(), any(), any())

        assertFalse(GestureExecutor().dispatchGesture(service, gesture()))
    }

    @Test
    fun dispatchGesture_lateCallbackAfterTimeout_doesNotCompleteNextGesture() = runTest {
        val service = mock(AccessibilityService::class.java)
        val executor = GestureExecutor()
        val callbackCaptor = ArgumentCaptor.forClass(GestureResultCallback::class.java)

        val timedOutResult = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service).dispatchGesture(any(), callbackCaptor.capture(), any())
        val timedOutCallback = callbackCaptor.value

        advanceTimeBy(200.milliseconds)
        assertFalse(timedOutResult.await())

        val nextResult = async { executor.dispatchGesture(service, gesture()) }
        runCurrent()
        verify(service, times(2)).dispatchGesture(any(), callbackCaptor.capture(), any())

        timedOutCallback.onCompleted(null)
        runCurrent()
        assertFalse(nextResult.isCompleted)

        callbackCaptor.allValues.last().onCompleted(null)

        assertTrue(nextResult.await())
    }

    @Test
    fun dispatchGestures_dispatchesWholeChainBeforeAwaitingResults() = runTest {
        val service = mock(AccessibilityService::class.java)
        val executor = GestureExecutor()
        val callbackCaptor = ArgumentCaptor.forClass(GestureResultCallback::class.java)

        val result = async { executor.dispatchGestures(service, listOf(gesture(), gesture(), gesture())) }
        runCurrent()

        // The whole chain must be handed to the system in one go: waiting for a result in between would freeze the
        // fingers on the screen until the next gesture is scheduled.
        verify(service, times(3)).dispatchGesture(any(), callbackCaptor.capture(), any())
        assertFalse(result.isCompleted)

        callbackCaptor.allValues.forEach { callback -> callback.onCompleted(null) }

        assertTrue(result.await())
    }

    @Test
    fun dispatchGestures_oneCancelledCallback_returnsFalse() = runTest {
        val service = mock(AccessibilityService::class.java)
        val executor = GestureExecutor()
        val callbackCaptor = ArgumentCaptor.forClass(GestureResultCallback::class.java)

        val result = async { executor.dispatchGestures(service, listOf(gesture(), gesture())) }
        runCurrent()
        verify(service, times(2)).dispatchGesture(any(), callbackCaptor.capture(), any())

        callbackCaptor.allValues.first().onCompleted(null)
        callbackCaptor.allValues.last().onCancelled(null)

        assertFalse(result.await())
    }

    @Test
    fun dispatchGestures_dispatchException_skipsRemainingGestures() = runTest {
        val service = mock(AccessibilityService::class.java)
        doThrow(IllegalStateException("Accessibility service is unavailable"))
            .`when`(service)
            .dispatchGesture(any(), any(), any())

        val result = GestureExecutor().dispatchGestures(service, listOf(gesture(), gesture(), gesture()))

        // Once the chain is broken, the remaining gestures would continue pointers that are no longer down.
        assertFalse(result)
        verify(service, times(1)).dispatchGesture(any(), any(), any())
    }

    private fun gesture(): GestureDescription = GestureDescription.Builder()
        .addStroke(
            GestureDescription.StrokeDescription(
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