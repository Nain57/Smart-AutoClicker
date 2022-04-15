/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.eventconfig.action

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast

import androidx.core.graphics.toPoint

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.menu.OverlayMenuController
import com.buzbuz.smartautoclicker.baseui.menu.overlayviews.ClickSelectorView
import com.buzbuz.smartautoclicker.baseui.menu.overlayviews.FIRST
import com.buzbuz.smartautoclicker.baseui.menu.overlayviews.SECOND
import com.buzbuz.smartautoclicker.baseui.menu.overlayviews.SelectionIndex

/**
 * [OverlayMenuController] implementation for displaying the click area selection menu and its overlay view.
 *
 * This class will display the overlay menu for selecting the positions for an action. The overlay view
 * displayed between the menu and the activity shows those positions.
 *
 * @param context the Android Context for the overlay menu shown by this controller.
 * @param selector the count of coordinates this overlay offers to select.
 * @param onCoordinatesSelected listener on the type of click and area(s) to user have selected.
 */
class ClickSwipeSelectorMenu(
    context: Context,
    private val selector: CoordinatesSelector,
    private val onCoordinatesSelected: (CoordinatesSelector) -> Unit
) : OverlayMenuController(context) {

    /** The view model for this dialog. */
    @SuppressLint("ClickableViewAccessibility")
    private val selectorView = ClickSelectorView(context).apply {
        onTouchListener = {
            setMenuItemViewEnabled(R.id.btn_confirm, true, true)
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        layoutInflater.inflate(R.layout.overlay_validation_menu, null) as ViewGroup

    override fun onCreateOverlayView(): ClickSelectorView = selectorView

    override fun onStart() {
        super.onStart()
        toSelectionStep(FIRST)
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> onCancel()
        }
    }

    /**
     * Move to the provided selection step.
     * @param step the selection step.
     */
    private fun toSelectionStep(@SelectionIndex step: Int) {
        selectorView.toSelectionStep(step)
        setMenuItemViewEnabled(R.id.btn_confirm, false)

        val toastStringId = when {
            step == FIRST && selector is CoordinatesSelector.One -> R.string.toast_configure_single_click
            step == FIRST && selector is CoordinatesSelector.Two -> R.string.toast_configure_swipe_from
            step == SECOND -> R.string.toast_configure_swipe_to
            else -> -1
        }
        if (toastStringId != -1) {
            Toast.makeText(context, toastStringId, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Confirm the position selected by the user and goes to the next selection step.
     * If this is the final step, notify the listener for the selection and dismiss the overlay.
     */
    private fun onConfirm() {
        when {
            selectorView.selectionStep == FIRST && selector is CoordinatesSelector.One -> {
                selector.coordinates = selectorView.position1!!.toPoint()
                onCoordinatesSelected.invoke(selector)
                dismiss()
            }
            selectorView.selectionStep == FIRST && selector is CoordinatesSelector.Two -> {
                toSelectionStep(SECOND)
            }
            selectorView.selectionStep == SECOND && selector is CoordinatesSelector.Two -> {
                selector.coordinates1 = selectorView.position1!!.toPoint()
                selector.coordinates2 = selectorView.position2!!.toPoint()
                onCoordinatesSelected.invoke(selector)
                dismiss()
            }
        }
    }

    /**
     * Cancel the position selected by the user and goes to the previous selection step.
     * If this is the initial step, dismiss the overlay without notifying the listener.
     */
    private fun onCancel() {
        when (selectorView.selectionStep) {
            FIRST -> dismiss()
            SECOND -> toSelectionStep(FIRST)
        }
    }
}

/** Indicates the type of coordinates to be selected. */
sealed class CoordinatesSelector {

    /** The user will pick one coordinate. */
    class One : CoordinatesSelector() {
        /** Selected coordinates. Null until the user select it. */
        var coordinates: Point? = null
    }

    /** The user will pick two coordinates. */
    class Two : CoordinatesSelector() {
        /** First selected coordinates. Null until the user select it. */
        var coordinates1: Point? = null
        /** Second selected coordinates. Null until the user select it. */
        var coordinates2: Point? = null
    }
}