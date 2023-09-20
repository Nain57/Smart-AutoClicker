/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.overlays.manager

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay
import com.buzbuz.smartautoclicker.core.ui.testutils.anyNotNull

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

/** Test the [OverlayManager] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayManagerTests {

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockSharedPreferences: SharedPreferences
    @Mock private lateinit var mockSharedPrefsEditor: SharedPreferences.Editor
    @Mock private lateinit var mockDisplayManager: DisplayManager
    @Mock private lateinit var mockDisplay: Display
    @Mock private lateinit var mockOverlay1: Overlay
    @Mock private lateinit var mockOverlay2: Overlay
    @Mock private lateinit var mockOverlay3: Overlay

    private lateinit var overlayManager: OverlayManager

    private inline fun <reified T : Any?> argumentCaptor() = ArgumentCaptor.forClass(T::class.java)

    private fun Overlay.setupDismissMock() {
        val cbCaptor = argumentCaptor<((Context, Overlay) -> Unit)?>()
        Mockito.verify(this).create(anyNotNull(), cbCaptor.capture())

        Mockito.doAnswer { cbCaptor.value?.invoke(mockContext, this) }
            .`when`(this).destroy()
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(mockContext.getSystemService(DisplayManager::class.java)).thenReturn(mockDisplayManager)
        Mockito.`when`(mockContext.getSharedPreferences(anyNotNull(), Mockito.anyInt())).thenReturn(mockSharedPreferences)
        Mockito.`when`(mockSharedPreferences.edit()).thenReturn(mockSharedPrefsEditor)
        Mockito.`when`(mockSharedPrefsEditor.putInt(Mockito.anyString(), Mockito.anyInt())).thenReturn(mockSharedPrefsEditor)
        Mockito.`when`(mockDisplayManager.getDisplay(0)).thenReturn(mockDisplay)

        overlayManager = OverlayManager(mockContext)
    }

    @Test
    fun stackTop_emptyStack() {
        Assert.assertNull(overlayManager.getBackStackTop())
    }

    @Test
    fun stackTop_navigateTo_initial() {
        overlayManager.navigateTo(mockContext, mockOverlay1)

        Assert.assertEquals(mockOverlay1, overlayManager.getBackStackTop())
    }

    @Test
    fun stackTop_navigateTo_second() {
        overlayManager.navigateTo(mockContext, mockOverlay1)

        overlayManager.navigateTo(mockContext, mockOverlay2)

        Assert.assertEquals(mockOverlay2, overlayManager.getBackStackTop())
    }

    @Test
    fun stackTop_navigateUp_initial() {
        overlayManager.navigateUp(mockContext)

        Assert.assertNull(overlayManager.getBackStackTop())
    }

    @Test
    fun stackTop_navigateUp_second() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.setupDismissMock()

        overlayManager.navigateUp(mockContext)

        Assert.assertNull(overlayManager.getBackStackTop())
    }

    @Test
    fun lifecycles_navigateTo_initialOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)

        Mockito.inOrder(mockOverlay1).apply {
            verify(mockOverlay1).create(anyNotNull(), anyNotNull())
            verify(mockOverlay1).start()
            verify(mockOverlay1).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1)
    }

    @Test
    fun lifecycles_navigateTo_noHide_secondOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        Mockito.clearInvocations(mockOverlay1)

        overlayManager.navigateTo(mockContext, mockOverlay2, hideCurrent = false)

        Mockito.inOrder(mockOverlay1, mockOverlay2).apply {
            verify(mockOverlay2).create(anyNotNull(), anyNotNull())
            verify(mockOverlay1).pause()
            verify(mockOverlay2).start()
            verify(mockOverlay2).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2)
    }

    @Test
    fun lifecycles_navigateTo_hide_secondOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        Mockito.clearInvocations(mockOverlay1)

        overlayManager.navigateTo(mockContext, mockOverlay2, hideCurrent = true)

        Mockito.inOrder(mockOverlay1, mockOverlay2).apply {
            verify(mockOverlay2).create(anyNotNull(), anyNotNull())
            verify(mockOverlay1).pause()
            verify(mockOverlay1).stop()
            verify(mockOverlay2).start()
            verify(mockOverlay2).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2)
    }

    @Test
    fun lifecycles_navigateTo_noHide_thirdOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        overlayManager.navigateTo(mockContext, mockOverlay2, hideCurrent = false)
        Mockito.clearInvocations(mockOverlay1, mockOverlay2)

        overlayManager.navigateTo(mockContext, mockOverlay3, hideCurrent = false)

        Mockito.inOrder(mockOverlay2, mockOverlay3).apply {
            verify(mockOverlay3).create(anyNotNull(), anyNotNull())
            verify(mockOverlay2).pause()
            verify(mockOverlay3).start()
            verify(mockOverlay3).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2, mockOverlay3)
    }

    @Test
    fun lifecycles_navigateTo_hide_thirdOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        overlayManager.navigateTo(mockContext, mockOverlay2, hideCurrent = false)
        Mockito.clearInvocations(mockOverlay1, mockOverlay2)

        overlayManager.navigateTo(mockContext, mockOverlay3, hideCurrent = true)

        Mockito.inOrder(mockOverlay2, mockOverlay3).apply {
            verify(mockOverlay3).create(anyNotNull(), anyNotNull())
            verify(mockOverlay2).pause()
            verify(mockOverlay2).stop()
            verify(mockOverlay3).start()
            verify(mockOverlay3).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2, mockOverlay3)
    }

    @Test
    fun lifecycles_navigateUp_oneOverlayInStack() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.setupDismissMock()
        Mockito.clearInvocations(mockOverlay1)

        overlayManager.navigateUp(mockContext)

        Mockito.inOrder(mockOverlay1).apply {
            verify(mockOverlay1).pause()
            verify(mockOverlay1).stop()
            verify(mockOverlay1).destroy()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1)
    }

    @Test
    fun lifecycles_navigateUp_twoOverlaysInStack() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        overlayManager.navigateTo(mockContext, mockOverlay2)
        mockOverlay2.setupDismissMock()
        Mockito.clearInvocations(mockOverlay1, mockOverlay2)

        overlayManager.navigateUp(mockContext)

        Mockito.inOrder(mockOverlay1, mockOverlay2).apply {
            verify(mockOverlay2).pause()
            verify(mockOverlay2).stop()
            verify(mockOverlay2).destroy()
            verify(mockOverlay1).resume()
        }
        Mockito.verifyNoMoreInteractions(mockOverlay1, mockOverlay2)
    }
}