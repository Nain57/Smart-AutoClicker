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
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.common

import android.os.Build
import android.view.View

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.annotation.Config

/** Test the [HorizontalSidePanelController] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class HorizontalSidePanelControllerTests {

    private val controller = HorizontalSidePanelController(
        parent = mock(ConstraintLayout::class.java),
        menuItems = mock(View::class.java),
        sidePanel = mock(View::class.java),
    )

    @Test
    fun chooseSide_whenRightSideFits_returnsRight() {
        val side = controller.chooseSide(
            anchorX = 100,
            anchorWidth = 100,
            screenWidth = 500,
            panelWidth = 200,
        )

        assertEquals(HorizontalSidePanelSide.RIGHT, side)
    }

    @Test
    fun chooseSide_whenRightSideDoesNotFitAndLeftSideFitsBetter_returnsLeft() {
        val side = controller.chooseSide(
            anchorX = 300,
            anchorWidth = 100,
            screenWidth = 500,
            panelWidth = 200,
        )

        assertEquals(HorizontalSidePanelSide.LEFT, side)
    }

    @Test
    fun chooseSide_whenRightSideDoesNotFitButLeftSideCannotFit_returnsRight() {
        val side = controller.chooseSide(
            anchorX = 150,
            anchorWidth = 100,
            screenWidth = 300,
            panelWidth = 200,
        )

        assertEquals(HorizontalSidePanelSide.RIGHT, side)
    }

    @Test
    fun chooseSide_whenRightSideDoesNotFitButLeftSideIsWorse_returnsRight() {
        val side = controller.chooseSide(
            anchorX = 150,
            anchorWidth = 100,
            screenWidth = 260,
            panelWidth = 120,
        )

        assertEquals(HorizontalSidePanelSide.RIGHT, side)
    }
}
