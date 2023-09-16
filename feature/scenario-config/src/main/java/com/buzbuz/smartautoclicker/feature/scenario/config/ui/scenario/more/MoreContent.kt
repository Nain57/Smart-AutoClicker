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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.scenario.more

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ContentMoreBinding
import com.buzbuz.smartautoclicker.feature.scenario.debugging.ui.report.DebugReportDialog

import kotlinx.coroutines.launch

class MoreContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: MoreViewModel by lazy { ViewModelProvider(this).get(MoreViewModel::class.java) }
    /** View binding for this content. */
    private lateinit var viewBinding: ContentMoreBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentMoreBinding.inflate(LayoutInflater.from(context), container, false).apply {
            tutorialCard.setOnClickListener { onTutorialClicked() }
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
                launch { viewModel.debugReportAvailability.collect(::updateDebugReportAvailability) }
            }
        }
    }

    override fun onDialogButtonClicked(buttonType: DialogNavigationButton) {
        if (buttonType == DialogNavigationButton.SAVE) {
            viewModel.saveConfig()
        }
    }

    private fun onTutorialClicked() {
        debounceUserInteraction {
            dialogController.back()

            val intent = Intent()
                .setComponent(ComponentName(context.packageName, "com.buzbuz.smartautoclicker.feature.tutorial.ui.TutorialActivity"))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    private fun updateDebugView(isEnabled: Boolean) {
        viewBinding.debugOverlay.isChecked = isEnabled
    }

    private fun updateDebugReport(isEnabled: Boolean) {
        viewBinding.debugReport.isChecked = isEnabled
    }

    private fun updateDebugReportAvailability(isAvailable: Boolean) {
        if (isAvailable) {
            viewBinding.debugReportStateText.setText(R.string.item_title_debug_report_available)
            viewBinding.debugReportChevron.visibility = View.VISIBLE
            viewBinding.debugReportOpenView.setOnClickListener { debounceUserInteraction { showDebugReport() } }
        } else {
            viewBinding.debugReportStateText.setText(R.string.item_title_debug_report_not_available)
            viewBinding.debugReportChevron.visibility = View.GONE
            viewBinding.debugReportOpenView.setOnClickListener(null)
        }
    }

    private fun showDebugReport() {
        OverlayManager.getInstance(context).navigateTo(
            context = context,
            newOverlay = DebugReportDialog(),
        )
    }
}