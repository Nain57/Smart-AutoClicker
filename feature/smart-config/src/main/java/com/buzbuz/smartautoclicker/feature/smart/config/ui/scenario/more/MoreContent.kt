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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.more

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setChecked
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setDescription
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setEnabled
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setOnClickListener
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setTitle
import com.buzbuz.smartautoclicker.core.ui.bindings.fields.setupDescriptions
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ContentMoreBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.report.DebugReportDialog

import kotlinx.coroutines.launch

class MoreContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for this content. */
    private val viewModel: MoreViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { moreViewModel() },
    )
    /** View binding for this content. */
    private lateinit var viewBinding: ContentMoreBinding

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewBinding = ContentMoreBinding.inflate(LayoutInflater.from(context), container, false).apply {
            fieldStartTutorial.apply {
                setTitle(context.getString(R.string.field_tutorial_title))
                setupDescriptions(listOf(context.getString(R.string.field_tutorial_desc)))
                setOnClickListener(::onTutorialClicked)
            }

            fieldDebugOverlay.apply {
                setTitle(context.getString(R.string.field_show_debug_view_title))
                setupDescriptions(emptyList())
                setOnClickListener(viewModel::toggleIsDebugViewEnabled)
            }

            fieldDebugReport.apply {
                setTitle(context.getString(R.string.item_title_debug_generate_report))
                setupDescriptions(emptyList())
                setOnClickListener(viewModel::toggleIsDebugReportEnabled)
            }

            fieldShowReport.apply {
                setTitle(context.getString(R.string.field_show_debug_report_title))
                setupDescriptions(
                    listOf(
                        context.getString(R.string.field_show_debug_report_desc_not_available),
                        context.getString(R.string.field_show_debug_report_desc_available),
                    )
                )
                setOnClickListener { debounceUserInteraction { showDebugReport() } }
            }
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
        viewBinding.fieldDebugOverlay.setChecked(isEnabled)
    }

    private fun updateDebugReport(isEnabled: Boolean) {
        viewBinding.fieldDebugReport.setChecked(isEnabled)
    }

    private fun updateDebugReportAvailability(isAvailable: Boolean) {
        viewBinding.fieldShowReport.apply {
            setEnabled(isAvailable)
            setDescription(if (isAvailable) 1 else 0)
        }
    }

    private fun showDebugReport() {
        dialogController.overlayManager.navigateTo(
            context = context,
            newOverlay = DebugReportDialog(),
        )
    }
}