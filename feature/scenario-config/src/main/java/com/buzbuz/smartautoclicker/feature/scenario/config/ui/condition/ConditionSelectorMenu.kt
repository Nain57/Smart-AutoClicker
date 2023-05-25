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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.IntDef
import androidx.lifecycle.ViewModelProvider

import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenuController
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.overlayviews.condition.ConditionSelectorView
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.OverlayValidationMenuBinding

/**
 * [OverlayMenuController] implementation for displaying the area selection menu and the area to be captured in order
 * to create a new event condition.
 *
 * @param context the Android Context for the overlay menu shown by this controller.
 * @param onConditionSelected listener upon confirmation of the area to be capture to create the event condition.
 */
class ConditionSelectorMenu(
    context: Context,
    private val onConditionSelected: (Rect, Bitmap) -> Unit
) : OverlayMenuController(context) {

    private companion object {
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
    private val selectorView = ConditionSelectorView(context, displayMetrics, ::onSelectorValidityChanged)

    /** The current state of the overlay. */
    @ConditionCaptureState
    private var state: Int = 0
        set(value) {
            field = value
            when (value) {
                SELECTION -> {
                    viewBinding.btnConfirm.setImageResource(R.drawable.ic_screenshot)
                    setMenuVisibility(View.VISIBLE)
                    setOverlayViewVisibility(View.GONE)
                    selectorView.hide = true
                }
                CAPTURE -> {
                    setMenuVisibility(View.GONE)
                    setOverlayViewVisibility(View.VISIBLE)
                    selectorView.hide = true
                }
                ADJUST -> {
                    viewBinding.btnConfirm.setImageResource(R.drawable.ic_confirm)
                    setMenuVisibility(View.VISIBLE)
                    selectorView.hide = false
                }
            }
        }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
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
        if (state == SELECTION) {
            state = CAPTURE

            val screenSize = displayMetrics.screenSize
            val screenRect = Rect(0, 0, screenSize.x, screenSize.y)
            viewModel.takeScreenshot(screenRect) { screenshot ->
                selectorView.showCapture(screenshot)
                state = ADJUST
            }
        } else {
            val selection = selectorView.getSelection()
            onConditionSelected(selection.first, selection.second)
            destroy()
        }
    }

    /**
     * Called when the user press the cancel button.
     * Depending on the current state, dismiss this overlay or return to the previous step.
     */
    private fun onCancel() {
        when (state) {
            SELECTION -> destroy()
            ADJUST -> state = SELECTION
        }
    }
}
