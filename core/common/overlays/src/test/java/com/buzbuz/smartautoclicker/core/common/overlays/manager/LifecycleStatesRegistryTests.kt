
package com.buzbuz.smartautoclicker.core.common.overlays.manager

import android.os.Build

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4

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

    @Mock private lateinit var mockCreatedOverlay: com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay
    @Mock private lateinit var mockStartedOverlay: com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay
    @Mock private lateinit var mockResumedOverlay: com.buzbuz.smartautoclicker.core.common.overlays.base.Overlay

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