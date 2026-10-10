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
package com.buzbuz.smartautoclicker.core.common.overlays.manager

import android.content.Context
import android.content.SharedPreferences
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display

import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common.OverlayMenuPositionDataSource
import com.buzbuz.smartautoclicker.core.common.overlays.other.FullscreenOverlay
import com.buzbuz.smartautoclicker.core.common.overlays.testutils.anyNotNull

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher

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
@OptIn(ExperimentalCoroutinesApi::class)
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
    @Mock private lateinit var mockTopOverlay: FullscreenOverlay
    @Mock private lateinit var mockKeyguardStateDataSource: KeyguardStateDataSource
    @Mock private lateinit var mockSettingsRepository: SettingsRepository

    private val keyguardVisibleFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private var showOnLockScreen: Boolean = false

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

        Mockito.`when`(mockKeyguardStateDataSource.isKeyguardVisibleFlow).thenReturn(keyguardVisibleFlow)
        Mockito.`when`(mockSettingsRepository.showOnLockScreen()).thenAnswer { showOnLockScreen }
        Mockito.`when`(mockOverlay1.context).thenReturn(mockContext)

        overlayManager = OverlayManager(
            mainDispatcher = UnconfinedTestDispatcher(),
            displayConfigManager = DisplayConfigManager(mockContext),
            menuPositionDataSource = OverlayMenuPositionDataSource(
                mockContext
            ),
            keyguardStateDataSource = mockKeyguardStateDataSource,
            settingsRepository = mockSettingsRepository,
        )
    }

    private fun Overlay.mockLifecycleState(state: Lifecycle.State) {
        val lifecycle = Mockito.mock(Lifecycle::class.java)
        Mockito.`when`(lifecycle.currentState).thenReturn(state)
        Mockito.`when`(this.lifecycle).thenReturn(lifecycle)
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

    @Test
    fun keyguard_visible_hidesStackFromTopToBottom() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        overlayManager.navigateTo(mockContext, mockOverlay2)
        mockOverlay1.mockLifecycleState(Lifecycle.State.STARTED)
        mockOverlay2.mockLifecycleState(Lifecycle.State.RESUMED)
        Mockito.clearInvocations(mockOverlay1, mockOverlay2)

        keyguardVisibleFlow.value = true

        Mockito.inOrder(mockOverlay1, mockOverlay2).apply {
            verify(mockOverlay2).hide()
            verify(mockOverlay1).hide()
        }
    }

    @Test
    fun keyguard_dismissed_restoresStackStates() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        overlayManager.navigateTo(mockContext, mockOverlay2)
        mockOverlay1.mockLifecycleState(Lifecycle.State.STARTED)
        mockOverlay2.mockLifecycleState(Lifecycle.State.RESUMED)
        keyguardVisibleFlow.value = true
        Mockito.clearInvocations(mockOverlay1, mockOverlay2)

        keyguardVisibleFlow.value = false

        Mockito.inOrder(mockOverlay1, mockOverlay2).apply {
            verify(mockOverlay1).start()
            verify(mockOverlay2).resume()
        }
        Mockito.verify(mockOverlay1, Mockito.never()).resume()
        Mockito.verify(mockOverlay2, Mockito.never()).start()
    }

    @Test
    fun keyguard_visible_showOnLockScreenEnabled() {
        showOnLockScreen = true
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        Mockito.clearInvocations(mockOverlay1)

        keyguardVisibleFlow.value = true

        Mockito.verify(mockOverlay1, Mockito.never()).hide()
    }

    @Test
    fun keyguard_alreadyVisible_firstOverlayShown() {
        keyguardVisibleFlow.value = true
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)

        overlayManager.navigateTo(mockContext, mockOverlay1)

        Mockito.verify(mockOverlay1).hide()
    }

    @Test
    fun keyguard_visible_navigateTo_newOverlayNotStarted() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        keyguardVisibleFlow.value = true

        overlayManager.navigateTo(mockContext, mockOverlay2)

        Mockito.verify(mockOverlay2).create(anyNotNull(), anyNotNull())
        Mockito.verify(mockOverlay2, Mockito.never()).start()
        Mockito.verify(mockOverlay2, Mockito.never()).resume()
    }

    @Test
    fun keyguard_dismissed_navigatedWhileVisible_newOverlayResumed() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        keyguardVisibleFlow.value = true
        overlayManager.navigateTo(mockContext, mockOverlay2)

        keyguardVisibleFlow.value = false

        Mockito.verify(mockOverlay2).resume()
    }

    @Test
    fun keyguard_visible_thenHideAll() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        keyguardVisibleFlow.value = true
        Mockito.clearInvocations(mockOverlay1)

        overlayManager.hideAll()

        Assert.assertTrue(overlayManager.isOverlayStackHidden())
        Mockito.verify(mockOverlay1, Mockito.never()).hide()
    }

    @Test
    fun keyguard_dismissed_afterHideAll_stackStaysHidden() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        keyguardVisibleFlow.value = true
        overlayManager.hideAll()
        Mockito.clearInvocations(mockOverlay1)

        keyguardVisibleFlow.value = false

        Assert.assertTrue(overlayManager.isOverlayStackHidden())
        Mockito.verify(mockOverlay1, Mockito.never()).resume()
    }

    @Test
    fun keyguard_dismissed_afterHideAll_restoreVisibility() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        keyguardVisibleFlow.value = true
        overlayManager.hideAll()
        keyguardVisibleFlow.value = false
        Mockito.clearInvocations(mockOverlay1)

        overlayManager.restoreVisibility()

        Assert.assertFalse(overlayManager.isOverlayStackHidden())
        Mockito.verify(mockOverlay1).resume()
    }

    @Test
    fun hideAll_thenKeyguardVisible() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        overlayManager.hideAll()
        Mockito.clearInvocations(mockOverlay1)

        keyguardVisibleFlow.value = true

        Mockito.verify(mockOverlay1, Mockito.never()).hide()
    }

    @Test
    fun hideAll_restoreVisibility_whileKeyguardVisible() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        overlayManager.hideAll()
        keyguardVisibleFlow.value = true
        Mockito.clearInvocations(mockOverlay1)

        overlayManager.restoreVisibility()

        Assert.assertFalse(overlayManager.isOverlayStackHidden())
        Mockito.verify(mockOverlay1, Mockito.never()).resume()
    }

    @Test
    fun hideAll_restoreVisibility_whileKeyguardVisible_thenDismissed() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        overlayManager.hideAll()
        keyguardVisibleFlow.value = true
        overlayManager.restoreVisibility()
        Mockito.clearInvocations(mockOverlay1)

        keyguardVisibleFlow.value = false

        Mockito.verify(mockOverlay1).resume()
    }

    @Test
    fun keyguard_visible_hidesTopOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        overlayManager.setTopOverlay(mockTopOverlay)
        Mockito.clearInvocations(mockTopOverlay)

        keyguardVisibleFlow.value = true

        Mockito.verify(mockTopOverlay).hide()
    }

    @Test
    fun keyguard_dismissed_showsTopOverlay() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        overlayManager.setTopOverlay(mockTopOverlay)
        keyguardVisibleFlow.value = true
        Mockito.clearInvocations(mockTopOverlay)

        keyguardVisibleFlow.value = false

        Mockito.verify(mockTopOverlay).show()
    }

    @Test
    fun keyguard_visible_setTopOverlay_notResumed() {
        overlayManager.navigateTo(mockContext, mockOverlay1)
        mockOverlay1.mockLifecycleState(Lifecycle.State.RESUMED)
        keyguardVisibleFlow.value = true

        overlayManager.setTopOverlay(mockTopOverlay)

        Mockito.verify(mockTopOverlay).create(anyNotNull(), anyNotNull())
        Mockito.verify(mockTopOverlay, Mockito.never()).resume()
    }
}