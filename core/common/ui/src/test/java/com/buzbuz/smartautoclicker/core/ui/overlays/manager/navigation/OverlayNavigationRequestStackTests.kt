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
package com.buzbuz.smartautoclicker.core.ui.overlays.manager.navigation

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.ui.overlays.Overlay

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.annotation.Config

/** Test the [OverlayNavigationRequestStack] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class OverlayNavigationRequestStackTests {

    @Mock private lateinit var mockOverlay1: Overlay
    @Mock private lateinit var mockOverlay2: Overlay

    private lateinit var testedStack: OverlayNavigationRequestStack

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        testedStack = OverlayNavigationRequestStack()
    }

    @Test
    fun pushNavigateUp_once() {
        testedStack.push(OverlayNavigationRequest.NavigateUp)

        Assert.assertEquals(OverlayNavigationRequest.NavigateUp, testedStack.top)
    }

    @Test
    fun pushNavigateUp_twice() {
        testedStack.apply {
            push(OverlayNavigationRequest.NavigateUp)
            push(OverlayNavigationRequest.NavigateUp)
        }

        Assert.assertEquals(OverlayNavigationRequest.NavigateUp, testedStack.top)
        Assert.assertEquals(2, testedStack.size)
    }

    @Test
    fun pushNavigateUp_cancelWithNavigateTo() {
        testedStack.apply {
            push(OverlayNavigationRequest.NavigateTo(mockOverlay1))
            push(OverlayNavigationRequest.NavigateUp)
        }

        Assert.assertEquals(null, testedStack.top)
        Assert.assertTrue(testedStack.isEmpty())
    }

    @Test
    fun pushNavigateTo_once() {
        val navigateToRequest = OverlayNavigationRequest.NavigateTo(mockOverlay1)

        testedStack.push(OverlayNavigationRequest.NavigateTo(mockOverlay1))

        Assert.assertEquals(navigateToRequest, testedStack.top)
    }

    @Test
    fun pushNavigateTo_twice_sameOverlay() {
        val navigateToRequest1 = OverlayNavigationRequest.NavigateTo(mockOverlay1)
        val navigateToRequest2 = OverlayNavigationRequest.NavigateTo(mockOverlay1)

        testedStack.apply {
            push(navigateToRequest1)
            push(navigateToRequest2)
        }

        Assert.assertEquals(navigateToRequest1, testedStack.top)
        Assert.assertEquals(1, testedStack.size)
    }

    @Test
    fun pushNavigateTo_twice_differentOverlay() {
        val navigateToRequest1 = OverlayNavigationRequest.NavigateTo(mockOverlay1)
        val navigateToRequest2 = OverlayNavigationRequest.NavigateTo(mockOverlay2)

        testedStack.apply {
            push(navigateToRequest1)
            push(navigateToRequest2)
        }

        Assert.assertEquals(navigateToRequest2, testedStack.top)
        Assert.assertEquals(2, testedStack.size)
    }
}