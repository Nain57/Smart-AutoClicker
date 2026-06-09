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
package com.buzbuz.smartautoclicker.core.common.actions

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.common.actions.gesture.GestureExecutor
import com.buzbuz.smartautoclicker.core.common.actions.notification.NotificationRequestExecutor
import com.buzbuz.smartautoclicker.core.common.actions.text.TextExecutor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class AndroidActionExecutorImplTests {

    @Mock private lateinit var mockGestureExecutor: GestureExecutor
    @Mock private lateinit var mockNotificationRequestExecutor: NotificationRequestExecutor
    @Mock private lateinit var mockTextExecutor: TextExecutor
    @Mock private lateinit var mockAccessibilityService: AccessibilityService
    @Mock private lateinit var mockGestureDescription: GestureDescription
    @Mock private lateinit var mockListener: GestureDispatchListener

    private lateinit var actionExecutor: AndroidActionExecutorImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        actionExecutor = AndroidActionExecutorImpl(
            mockGestureExecutor,
            mockNotificationRequestExecutor,
            mockTextExecutor,
        )
    }

    @Test
    fun dispatchGesture_notifiesListener() = runTest {
        actionExecutor.init(mockAccessibilityService)
        actionExecutor.setGestureDispatchListener(mockListener)

        `when`(mockGestureExecutor.dispatchGesture(mockAccessibilityService, mockGestureDescription)).thenReturn(true)

        actionExecutor.dispatchGesture(mockGestureDescription)

        inOrder(mockListener, mockGestureExecutor).apply {
            verify(mockListener).onGestureWillDispatch()
            verify(mockGestureExecutor).dispatchGesture(mockAccessibilityService, mockGestureDescription)
            verify(mockListener).onGestureDidDispatch()
        }
    }

    @Test
    fun dispatchGesture_notifiesListenerOnFailure() = runTest {
        actionExecutor.init(mockAccessibilityService)
        actionExecutor.setGestureDispatchListener(mockListener)

        `when`(mockGestureExecutor.dispatchGesture(mockAccessibilityService, mockGestureDescription)).thenAnswer {
            throw RuntimeException("Test error")
        }

        try {
            actionExecutor.dispatchGesture(mockGestureDescription)
        } catch (e: Exception) {
            // expected
        }

        inOrder(mockListener, mockGestureExecutor).apply {
            verify(mockListener).onGestureWillDispatch()
            verify(mockGestureExecutor).dispatchGesture(mockAccessibilityService, mockGestureDescription)
            verify(mockListener).onGestureDidDispatch()
        }
    }
}
