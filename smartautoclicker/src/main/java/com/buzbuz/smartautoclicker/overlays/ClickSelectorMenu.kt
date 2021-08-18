/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.overlays

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast

import androidx.core.graphics.toPoint

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.overlays.OverlayMenuController
import com.buzbuz.smartautoclicker.baseui.overlayviews.ClickSelectorView
import com.buzbuz.smartautoclicker.baseui.overlayviews.ClickSelectorView.Companion.FIRST
import com.buzbuz.smartautoclicker.baseui.overlayviews.ClickSelectorView.Companion.SECOND
import com.buzbuz.smartautoclicker.baseui.overlayviews.ClickSelectorView.Companion.SelectionIndex
import com.buzbuz.smartautoclicker.database.ClickInfo

/**
 * [OverlayMenuController] implementation for displaying the click area selection menu and its overlay view.
 *
 * This class will display the overlay menu for selecting the area (or areas for swipe) for a click. The overlay view
 * displayed between the menu and the activity shows those areas.
 *
 * @param context the Android Context for the overlay menu shown by this controller.
 * @param type the type of click this overlay offers to select.
 * @param onClickSelectedListener listener on the type of click and area(s) to user have selected.
 */
class ClickSelectorMenu(
    context: Context,
    @ClickInfo.Companion.ClickType private val type: Int,
    private val onClickSelectedListener: (Int, Point, Point?) -> Unit
) : OverlayMenuController(context) {

    /** */
    @SuppressLint("ClickableViewAccessibility")
    private val selectorView = ClickSelectorView(context).apply {
        onTouchListener = {
            setMenuItemViewEnabled(R.id.btn_confirm, true, true)
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        layoutInflater.inflate(R.layout.overlay_validation_menu, null) as ViewGroup

    override fun onCreateOverlayView(): ClickSelectorView = selectorView

    override fun onShow() {
        super.onShow()
        toSelectionStep(FIRST)
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> onCancel()
        }
    }

    private fun toSelectionStep(@SelectionIndex step: Int) {
        selectorView.toSelectionStep(step)
        setMenuItemViewEnabled(R.id.btn_confirm, false)

        val toastStringId = when {
            step == FIRST && type == ClickInfo.SINGLE -> R.string.toast_configure_single_click
            step == FIRST && type == ClickInfo.SWIPE -> R.string.toast_configure_swipe_from
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
            selectorView.selectionStep == FIRST && type == ClickInfo.SINGLE -> {
                onClickSelectedListener.invoke(
                    type,
                    selectorView.position1!!.toPoint(),
                    selectorView.position2?.toPoint()
                )
                dismiss()
            }
            selectorView.selectionStep == FIRST && type == ClickInfo.SWIPE -> {
                toSelectionStep(SECOND)
            }
            selectorView.selectionStep == SECOND -> {
                onClickSelectedListener.invoke(
                    type,
                    selectorView.position1!!.toPoint(),
                    selectorView.position2!!.toPoint()
                )
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