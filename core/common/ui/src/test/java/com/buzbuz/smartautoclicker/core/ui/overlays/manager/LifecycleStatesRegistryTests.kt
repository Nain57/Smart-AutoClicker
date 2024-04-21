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

import android.os.Build

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

/** Test the [LifecycleStatesRegistry] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class LifecycleStatesRegistryTests {

    @Mock private lateinit var mockCreatedOverlay: Overlay
    @Mock private lateinit var mockStartedOverlay: Overlay
    @Mock private lateinit var mockResumedOverlay: Overlay

    private lateinit var lifecycleStatesRegistry: LifecycleStatesRegistry

    private fun LifecycleOwner.mockLifecycle(state: Lifecycle.State) {
        Mockito.mock(Lifecycle::class.java).let { mockLifecycle ->
            Mockito.`when`(lifecycle).thenReturn(mockLifecycle)
            Mockito.`when`(mockLifecycle.currentState).thenReturn(state)
        }
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockCreatedOverlay.mockLifecycle(Lifecycle.State.CREATED)
        mockStartedOverlay.mockLifecycle(Lifecycle.State.STARTED)
        mockResumedOverlay.mockLifecycle(Lifecycle.State.RESUMED)

        lifecycleStatesRegistry = LifecycleStatesRegistry()
    }

    @Test
    fun saveAndRestore() {
        val overlays = listOf(
            mockCreatedOverlay,
            mockStartedOverlay,
            mockResumedOverlay,
        )

        lifecycleStatesRegistry.saveStates(overlays)

        lifecycleStatesRegistry.restoreStates().let { states ->
            Assert.assertEquals(Lifecycle.State.CREATED, states[mockCreatedOverlay])
            Assert.assertEquals(Lifecycle.State.STARTED, states[mockStartedOverlay])
            Assert.assertEquals(Lifecycle.State.RESUMED, states[mockResumedOverlay])
        }
    }

    @Test
    fun saveAndRestore_erasePreviousValues() {
        val firstOverlays = listOf(
            mockCreatedOverlay,
            mockStartedOverlay,
            mockResumedOverlay,
        )
        val secondOverlays = listOf(
            mockCreatedOverlay,
            mockResumedOverlay,
        )

        lifecycleStatesRegistry.saveStates(firstOverlays)
        lifecycleStatesRegistry.saveStates(secondOverlays)

        lifecycleStatesRegistry.restoreStates().let { states ->
            Assert.assertEquals(Lifecycle.State.CREATED, states[mockCreatedOverlay])
            Assert.assertEquals(null, states[mockStartedOverlay])
            Assert.assertEquals(Lifecycle.State.RESUMED, states[mockResumedOverlay])
        }
    }
}