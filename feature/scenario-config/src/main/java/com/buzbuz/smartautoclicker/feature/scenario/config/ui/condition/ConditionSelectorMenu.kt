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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.condition

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.IntDef
import androidx.lifecycle.ViewModelProvider

import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.views.conditionselector.ConditionSelectorView
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.OverlayValidationMenuBinding

/**
 * [OverlayMenu] implementation for displaying the area selection menu and the area to be captured in order
 * to create a new event condition.
 *
 * @param onConditionSelected listener upon confirmation of the area to be capture to create the event condition.
 */
class ConditionSelectorMenu(
    private val onConditionSelected: (Rect, Bitmap) -> Unit
) : OverlayMenu() {

    private companion object {

        /** Tag for logs */
        private const val TAG = "ConditionSelectorMenu"

        /** Describe the state of the capture. */
        @IntDef(SELECTION, CAPTURE, ADJUST)
        @Retention(AnnotationRetention.SOURCE)
        private annotation class ConditionCaptureState
        /** User is selecting the screenshot to take. */
        private const val SELECTION = 1
        /** User have clicked on the capture button and we are waiting for the screenshot result. */
        private const val CAPTURE = 2
        /** User is selecting a part of the capture for the event condition. */
        private const val ADJUST = 3
    }

    /** The view model for this menu. */
    private val viewModel: ConditionSelectorViewModel by lazy {
        ViewModelProvider(this).get(ConditionSelectorViewModel::class.java)
    }

    /** The view binding for the overlay menu. */
    private lateinit var viewBinding: OverlayValidationMenuBinding
    /** The view displaying the screenshot and the selector for the capture. */
    private lateinit var selectorView: ConditionSelectorView

    /** The current state of the overlay. */
    @ConditionCaptureState
    private var state: Int = 0
        set(value) {
            field = value
            when (value) {
                SELECTION -> {
                    viewBinding.btnConfirm.setImageResource(R.drawable.ic_screenshot)
                    setMenuVisibility(View.VISIBLE)
                    setOverlayViewVisibility(false)
                    selectorView.hide = true
                }
                CAPTURE -> {
                    setMenuVisibility(View.GONE)
                    setOverlayViewVisibility(true)
                    selectorView.hide = true
                }
                ADJUST -> {
                    viewBinding.btnConfirm.setImageResource(R.drawable.ic_confirm)
                    setMenuVisibility(View.VISIBLE)
                    selectorView.hide = false
                }
            }
        }

    override fun animateOverlayView(): Boolean = false

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        selectorView = ConditionSelectorView(context, displayMetrics, ::onSelectorValidityChanged)
        viewBinding = OverlayValidationMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateOverlayView(): View = selectorView

    override fun onStart() {
        super.onStart()
        state = SELECTION
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> onCancel()
        }
    }

    /**
     * Called when the validity of the selector have changed.
     * Update the buttons to avoid a capture if the selector can't provide the bitmap.
     *
     * @param isValid validity of the selector.
     */
    private fun onSelectorValidityChanged(isValid: Boolean) {
        setMenuItemViewEnabled(viewBinding.btnConfirm, isValid, isValid)
    }

    /**
     * Called when the user press the confirmation button.
     * Depending on the current [state], this will have different effect.
     */
    private fun onConfirm() {
        when (state) {
            SELECTION -> {
                state = CAPTURE
                viewModel.takeScreenshot { screenshot ->
                    selectorView.showCapture(screenshot)
                    state = ADJUST
                }
            }

            ADJUST -> {
                back()
                try {
                    val selection = selectorView.getSelection()
                    onConditionSelected(selection.first, selection.second)
                } catch (ex: IllegalStateException) {
                    Log.e(TAG, "Condition selection failed", ex)
                }
            }
        }
    }

    /**
     * Called when the user press the cancel button.
     * Depending on the current state, dismiss this overlay or return to the previous step.
     */
    private fun onCancel() {
        when (state) {
            SELECTION -> back()
            ADJUST -> state = SELECTION
        }
    }
}
