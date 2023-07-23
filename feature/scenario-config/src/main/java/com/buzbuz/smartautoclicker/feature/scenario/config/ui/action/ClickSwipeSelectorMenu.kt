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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action

import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout

import androidx.core.graphics.toPoint

import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.views.FIRST
import com.buzbuz.smartautoclicker.core.ui.views.SECOND
import com.buzbuz.smartautoclicker.core.ui.views.SelectionIndex
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.OverlayPositionSelectionMenuBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.OverlayPositionSelectionViewBinding

/**
 * [OverlayMenu] implementation for displaying the click area selection menu and its overlay view.
 *
 * This class will display the overlay menu for selecting the positions for an action. The overlay view
 * displayed between the menu and the activity shows those positions.
 *
 * @param selector the count of coordinates this overlay offers to select.
 * @param onCoordinatesSelected listener on the type of click and area(s) to user have selected.
 */
class ClickSwipeSelectorMenu(
    private val selector: CoordinatesSelector,
    private val onCoordinatesSelected: (CoordinatesSelector) -> Unit
) : OverlayMenu() {

    /** The view binding for the overlay menu. */
    private lateinit var viewBinding: OverlayPositionSelectionMenuBinding
    /** The view binding for the position selector. */
    private lateinit var selectorViewBinding: OverlayPositionSelectionViewBinding

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewBinding = OverlayPositionSelectionMenuBinding.inflate(layoutInflater)
        selectorViewBinding = OverlayPositionSelectionViewBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateOverlayView(): View {
        selectorViewBinding.positionSelector.apply {
            onTouchListener = {
                setMenuItemViewEnabled(viewBinding.btnConfirm, true, true)
            }
        }

        selectorViewBinding.textInstructions.layoutParams =
            (selectorViewBinding.textInstructions.layoutParams as ConstraintLayout.LayoutParams).apply {
                setMargins(leftMargin, topMargin + displayMetrics.safeInsetTop, rightMargin, bottomMargin)
            }

        return selectorViewBinding.root
    }

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
        selectorViewBinding.positionSelector.toSelectionStep(step)
        setMenuItemViewEnabled(viewBinding.btnConfirm, false)

        val instructionsStringId = when {
            step == FIRST && selector is CoordinatesSelector.One -> R.string.toast_configure_single_click
            step == FIRST && selector is CoordinatesSelector.Two -> R.string.toast_configure_swipe_from
            step == SECOND -> R.string.toast_configure_swipe_to
            else -> -1
        }
        if (instructionsStringId != -1) {
            selectorViewBinding.textInstructions.setText(instructionsStringId)
        }
    }

    /**
     * Confirm the position selected by the user and goes to the next selection step.
     * If this is the final step, notify the listener for the selection and dismiss the overlay.
     */
    private fun onConfirm() {
        when {
            selectorViewBinding.positionSelector.selectionStep == FIRST && selector is CoordinatesSelector.One -> {
                selector.coordinates = selectorViewBinding.positionSelector.position1!!.toPoint()
                back()
                onCoordinatesSelected(selector)
            }
            selectorViewBinding.positionSelector.selectionStep == FIRST && selector is CoordinatesSelector.Two -> {
                toSelectionStep(SECOND)
            }
            selectorViewBinding.positionSelector.selectionStep == SECOND && selector is CoordinatesSelector.Two -> {
                selector.coordinates1 = selectorViewBinding.positionSelector.position1!!.toPoint()
                selector.coordinates2 = selectorViewBinding.positionSelector.position2!!.toPoint()
                back()
                onCoordinatesSelected(selector)
            }
        }
    }

    /**
     * Cancel the position selected by the user and goes to the previous selection step.
     * If this is the initial step, dismiss the overlay without notifying the listener.
     */
    private fun onCancel() {
        when (selectorViewBinding.positionSelector.selectionStep) {
            FIRST -> back()
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