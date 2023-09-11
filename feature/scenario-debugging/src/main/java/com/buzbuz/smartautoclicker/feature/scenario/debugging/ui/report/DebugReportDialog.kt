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
package com.buzbuz.smartautoclicker.feature.scenario.debugging.ui.report

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.feature.scenario.debugging.R
import com.buzbuz.smartautoclicker.feature.scenario.debugging.databinding.DialogDebugReportBinding
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/** Displays the content of the current debug report. */
class DebugReportDialog : OverlayDialog(R.style.AppTheme) {

    /** View model for this dialog. */
    private val viewModel: DebugReportModel by viewModels()

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogDebugReportBinding
    /** View binding for all views in this content. */
    private lateinit var listBinding: IncludeLoadableListBinding
    /** Adapter for the report */
    private lateinit var reportAdapter: DebugReportAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogDebugReportBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_debug_report)
                buttonSave.visibility = View.GONE
                buttonDismiss.setOnClickListener { back() }
            }
        }

        listBinding = viewBinding.layoutList
        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        reportAdapter = DebugReportAdapter(
            bitmapProvider = viewModel::getConditionBitmap,
            onConditionClicked = ::showConditionReportDialog,
        )
        listBinding.list.adapter = reportAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportItems.collect(::updateReport)
            }
        }
    }

    private fun updateReport(reportItems: List<DebugReportItem>) {
        listBinding.updateState(reportItems)
        reportAdapter.submitList(reportItems)
    }

    private fun showConditionReportDialog(conditionReport: ConditionReport) {
        OverlayManager.getInstance(context)
            .navigateTo(
                context = context,
                newOverlay = DebugReportConditionDialog(conditionReport),
            )
    }
}