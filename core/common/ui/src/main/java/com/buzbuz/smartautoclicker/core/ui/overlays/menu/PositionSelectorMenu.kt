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
package com.buzbuz.smartautoclicker.core.ui.overlays.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.constraintlayout.widget.ConstraintLayout

import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.databinding.OverlayPositionSelectionMenuBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.OverlayPositionSelectionViewBinding
import com.buzbuz.smartautoclicker.core.ui.utils.AutoHideAnimationController
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ActionDescription
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.ClickDescription
import com.buzbuz.smartautoclicker.core.ui.views.actionbrief.SwipeDescription

/**
 * [OverlayMenu] implementation for displaying the click area selection menu and its overlay view.
 *
 * This class will display the overlay menu for selecting the positions for an action. The overlay view
 * displayed between the menu and the activity shows those positions.
 *
 * @param actionDescription the description of the action positions to edit.
 * @param onConfirm listener on the validation of the actions positions.
 * @param onDismiss listener on the dismiss of the position selection.
 */
class PositionSelectorMenu(
    private val actionDescription: ActionDescription,
    private val onConfirm: (ActionDescription) -> Unit,
    private val onDismiss: (() -> Unit)? = null,
) : OverlayMenu() {

    /** The view binding for the overlay menu. */
    private lateinit var viewBinding: OverlayPositionSelectionMenuBinding
    /** The view binding for the position selector. */
    private lateinit var selectorViewBinding: OverlayPositionSelectionViewBinding

    /** Controls the instructions in and out animations. */
    private lateinit var instructionsAnimationController: AutoHideAnimationController

    private var confirmListener: (() -> Unit)? = null
    private var cancelListener: (() -> Unit)? = null

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        viewBinding = OverlayPositionSelectionMenuBinding.inflate(layoutInflater)
        selectorViewBinding = OverlayPositionSelectionViewBinding.inflate(layoutInflater)

        instructionsAnimationController = AutoHideAnimationController().apply {
            attachToView(
                selectorViewBinding.layoutInstructions,
                AutoHideAnimationController.ScreenSide.TOP,
            )
        }

        return viewBinding.root
    }

    override fun onCreateOverlayView(): View {
        selectorViewBinding.textInstructions.layoutParams =
            (selectorViewBinding.textInstructions.layoutParams as ConstraintLayout.LayoutParams).apply {
                setMargins(leftMargin, topMargin + displayMetrics.safeInsetTop, rightMargin, bottomMargin)
            }

        return selectorViewBinding.root
    }

    override fun onStart() {
        super.onStart()
        setActionDescription(actionDescription)
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        if (isVisible) instructionsAnimationController.showOrResetTimer()
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> confirmListener?.invoke()
            R.id.btn_cancel -> cancelListener?.invoke()
        }
    }

    private fun setActionDescription(description: ActionDescription) {
        when (description) {
            is ClickDescription -> setClickDescription(description)
            is SwipeDescription -> setSwipeDescription(description)
        }

        instructionsAnimationController.showOrResetTimer()
    }

    private fun setClickDescription(description: ClickDescription) {
        selectorViewBinding.textInstructions.setText(R.string.toast_configure_single_click)
        selectorViewBinding.positionSelector.apply {
            setDescription(description)
            onTouchListener = { position ->
                setClickDescription(description.copy(position = position))
            }
        }

        setConfirmEnabledState(description.position != null) {
            onPositionSelectionCompleted(description)
        }
        setCancelListener {
            dismiss()
        }
    }

    private fun setSwipeDescription(description: SwipeDescription) {
        if (description.from == null) {
            toSelectSwipeFromState(description)
        } else {
            toSelectSwipeToState(description)
        }
    }

    private fun toSelectSwipeFromState(description: SwipeDescription) {
        selectorViewBinding.textInstructions.setText(R.string.toast_configure_swipe_from)
        selectorViewBinding.positionSelector.apply {
            setDescription(description)
            onTouchListener = { position ->
                toSelectSwipeFromState(description.copy(from = position))
            }
        }

        setConfirmEnabledState(description.from != null) {
            toSelectSwipeToState(description)
            instructionsAnimationController.showOrResetTimer()
        }
        setCancelListener {
            dismiss()
        }
    }

    private fun toSelectSwipeToState(description: SwipeDescription) {
        selectorViewBinding.textInstructions.setText(R.string.toast_configure_swipe_to)
        selectorViewBinding.positionSelector.apply {
            setDescription(description)
            onTouchListener = { position ->
                toSelectSwipeToState(description.copy(to = position))
            }
        }

        setConfirmEnabledState(description.to != null) {
            onPositionSelectionCompleted(description)
        }
        setCancelListener {
            toSelectSwipeFromState(description.copy(to = null))
            instructionsAnimationController.showOrResetTimer()
        }
    }

    private fun onPositionSelectionCompleted(description: ActionDescription) {
        back()
        onConfirm(description)
    }

    private fun dismiss() {
        back()
        onDismiss?.invoke()
    }

    private fun setConfirmEnabledState(isEnabled: Boolean, action: (() -> Unit)? = null) {
        confirmListener = action
        setMenuItemViewEnabled(viewBinding.btnConfirm, enabled = isEnabled, clickable = isEnabled)
    }

    private fun setCancelListener(action: (() -> Unit)) {
        cancelListener = action
    }
}
