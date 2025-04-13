/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.image

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.IntDef

import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.views.conditionselector.ConditionSelectorView
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayValidationMenuBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint

/**
 * [OverlayMenu] implementation for displaying the area selection menu and the area to be captured in order
 * to create a new event condition.
 *
 * @param onConditionSelected listener upon confirmation of the area to be capture to create the event condition.
 */
class ImageConditionCaptureMenu(
    private val onConditionSelected: (ImageCondition) -> Unit
) : OverlayMenu() {

    private companion object {

        /** Tag for logs */
        private const val TAG = "ConditionSelectorMenu"

        /** Describe the state of the capture. */
        @IntDef(SELECTION, CAPTURE, ADJUST, SAVE)
        @Retention(AnnotationRetention.SOURCE)
        private annotation class ConditionCaptureState
        /** User is selecting the screenshot to take. */
        private const val SELECTION = 1
        /** User have clicked on the capture button and we are waiting for the screenshot result. */
        private const val CAPTURE = 2
        /** User is selecting a part of the capture for the event condition. */
        private const val ADJUST = 3
        /** User is selecting a part of the capture for the event condition. */
        private const val SAVE = 4
    }

    /** The view model for this menu. */
    private val viewModel: ImageConditionCaptureViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { captureViewModel() },
    )

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
                    viewBinding.btnConfirm.setImageResource(R.drawable.ic_capture)
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
                SAVE -> {
                    setMenuItemViewEnabled(viewBinding.btnConfirm, false)
                    setMenuItemViewEnabled(viewBinding.btnCancel, false)
                    selectorView.hide = false
                }
            }
        }

    override fun animateOverlayView(): Boolean = false

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        selectorView = ConditionSelectorView(context, displayConfigManager, ::onSelectorValidityChanged)
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
                state = SAVE
                try {
                    val selection = selectorView.getSelection()
                    viewModel.createImageCondition(context, selection.first, selection.second) { imageCondition ->
                        back()
                        onConditionSelected(imageCondition)
                    }
                } catch (ex: IllegalStateException) {
                    Log.e(TAG, "Condition selection failed", ex)
                    state = ADJUST
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
