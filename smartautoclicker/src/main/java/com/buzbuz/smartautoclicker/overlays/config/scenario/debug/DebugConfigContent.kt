/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.scenario.debug

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.databinding.ContentDebugConfigBinding
import com.buzbuz.smartautoclicker.overlays.base.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.base.bindings.DialogNavigationButton

import kotlinx.coroutines.launch

class DebugConfigContent : NavBarDialogContent() {

    /** View model for this content. */
    private val viewModel: DebugConfigViewModel by lazy { ViewModelProvider(this).get(DebugConfigViewModel::class.java) }
    /** View binding for this content. */
    private lateinit var viewBinding: ContentDebugConfigBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentDebugConfigBinding.inflate(LayoutInflater.from(context), container, false).apply {
            debugOverlay.setOnClickListener { viewModel.toggleIsDebugViewEnabled() }
            debugReport.setOnClickListener { viewModel.toggleIsDebugReportEnabled() }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.isDebugViewEnabled.collect(::updateDebugView) }
                launch { viewModel.isDebugReportEnabled.collect(::updateDebugReport) }
            }
        }
    }

    override fun onDialogButtonClicked(buttonType: DialogNavigationButton) {
        if (buttonType == DialogNavigationButton.SAVE) {
            viewModel.saveConfig()
        }
    }

    private fun updateDebugView(isEnabled: Boolean) {
        viewBinding.debugOverlay.isChecked = isEnabled
    }

    private fun updateDebugReport(isEnabled: Boolean) {
        viewBinding.debugReport.isChecked = isEnabled
    }
}