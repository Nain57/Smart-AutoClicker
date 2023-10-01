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

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.views.areaselector.AreaSelectorView
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.OverlayValidationMenuBinding

import kotlinx.coroutines.launch

class ConditionAreaSelectorMenu(
    private val onAreaSelected: (Rect) -> Unit
) : OverlayMenu() {

    /** The view model for this dialog. */
    private val viewModel: ConditionAreaSelectorViewModel by viewModels()

    /** The view binding for the overlay menu. */
    private lateinit var viewBinding: OverlayValidationMenuBinding
    /** The view displaying selector for the area. */
    private lateinit var selectorView: AreaSelectorView

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        selectorView = AreaSelectorView(context, displayMetrics)
        viewBinding = OverlayValidationMenuBinding.inflate(layoutInflater)
        return viewBinding.root
    }

    override fun onCreateOverlayView(): View = selectorView

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.initialArea.collect { selectorState ->
                    selectorView.setSelection(selectorState.initialArea, selectorState.minimalArea)
                }
            }
        }
    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_confirm -> onConfirm()
            R.id.btn_cancel -> onCancel()
        }
    }

    /** Called when the user press the confirmation button. */
    private fun onConfirm() {
        onAreaSelected(selectorView.getSelection())
        back()
    }

    /** Called when the user press the cancel button. */
    private fun onCancel() {
        back()
    }
}