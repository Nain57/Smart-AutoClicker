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
package com.buzbuz.smartautoclicker.overlays.eventconfig.condition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.IntDef

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.menu.OverlayMenuController
import com.buzbuz.smartautoclicker.baseui.menu.overlayviews.condition.ConditionSelectorView
import com.buzbuz.smartautoclicker.engine.DetectorEngine

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
        /** Delay before confirming the selection in order to let the time to the selector view to be hide. */
        private const val SELECTION_DELAY_MS = 200L

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

    /** The view displaying the screenshot and the selector for the capture. */
    private val selectorView = ConditionSelectorView(context, screenMetrics, ::onSelectorValidityChanged)

    /** The current state of the overlay. */
    @ConditionCaptureState
    private var state: Int = 0
        set(value) {
            field = value
            when (value) {
                SELECTION -> {
                    setMenuItemViewImageResource(R.id.btn_confirm, R.drawable.ic_screenshot)
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
                    setMenuItemViewImageResource(R.id.btn_confirm, R.drawable.ic_confirm)
                    setMenuVisibility(View.VISIBLE)
                    selectorView.hide = false
                }
            }
        }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup =
        layoutInflater.inflate(R.layout.overlay_validation_menu, null) as ViewGroup

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
        setMenuItemViewEnabled(R.id.btn_confirm, isValid, isValid)
    }

    /**
     * Called when the user press the confirmation button.
     * Depending on the current [state], this will have different effect.
     */
    private fun onConfirm() {
        if (state == SELECTION) {
            state = CAPTURE
            Handler(Looper.getMainLooper()).postDelayed({
                val screenSize = screenMetrics.screenSize
                val screenRect = Rect(0, 0, screenSize.x, screenSize.y)
                DetectorEngine.getDetectorEngine(context).captureArea(screenRect) { bitmap ->
                    selectorView.showCapture(bitmap)
                    state = ADJUST
                }
            }, SELECTION_DELAY_MS)
        } else {
            val selection = selectorView.getSelection()
            onConditionSelected(selection.first, selection.second)
            dismiss()
        }
    }

    /**
     * Called when the user press the cancel button.
     * Depending on the current state, dismiss this overlay or return to the previous step.
     */
    private fun onCancel() {
        when (state) {
            SELECTION -> dismiss()
            ADJUST -> state = SELECTION
        }
    }
}
