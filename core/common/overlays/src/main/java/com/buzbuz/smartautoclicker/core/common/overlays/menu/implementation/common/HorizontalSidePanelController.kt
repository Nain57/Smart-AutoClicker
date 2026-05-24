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

import android.view.View

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

/** Side used to display a panel attached to an overlay menu button cluster. */
enum class HorizontalSidePanelSide {
    LEFT,
    RIGHT,
}

/**
 * Moves a side panel before or after the menu buttons inside a single rounded overlay container.
 */
class HorizontalSidePanelController(
    private val parent: ConstraintLayout,
    private val menuItems: View,
    private val sidePanel: View,
) {

    var currentSide: HorizontalSidePanelSide = HorizontalSidePanelSide.RIGHT
        private set

    fun chooseSide(
        anchorX: Int,
        anchorWidth: Int,
        screenWidth: Int,
        panelWidth: Int,
    ): HorizontalSidePanelSide {
        val rightSpace = screenWidth - anchorX - anchorWidth
        val leftSpace = anchorX

        return if (
            rightSpace < panelWidth &&
            leftSpace >= panelWidth &&
            leftSpace > rightSpace
        ) HorizontalSidePanelSide.LEFT
        else HorizontalSidePanelSide.RIGHT
    }

    fun applySide(side: HorizontalSidePanelSide) {
        if (currentSide == side) return

        ConstraintSet().apply {
            clone(parent)

            clear(menuItems.id, ConstraintSet.START)
            clear(menuItems.id, ConstraintSet.END)
            clear(sidePanel.id, ConstraintSet.START)
            clear(sidePanel.id, ConstraintSet.END)

            if (side == HorizontalSidePanelSide.LEFT) {
                connect(sidePanel.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(sidePanel.id, ConstraintSet.END, menuItems.id, ConstraintSet.START)
                connect(menuItems.id, ConstraintSet.START, sidePanel.id, ConstraintSet.END)
                connect(menuItems.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            } else {
                connect(menuItems.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(menuItems.id, ConstraintSet.END, sidePanel.id, ConstraintSet.START)
                connect(sidePanel.id, ConstraintSet.START, menuItems.id, ConstraintSet.END)
                connect(sidePanel.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            }

            applyTo(parent)
        }

        currentSide = side
    }
}
