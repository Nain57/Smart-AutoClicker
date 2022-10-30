/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.overlays.debugging

import android.content.Context
import android.view.LayoutInflater
import android.view.View

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.DialogDebugReportBinding
import com.buzbuz.smartautoclicker.overlays.base.utils.LoadableListDialog

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

class DebugReportDialog(context: Context): LoadableListDialog(context) {

    /** View model for this dialog. */
    private val viewModel: DebugReportModel by lazy { ViewModelProvider(this).get(DebugReportModel::class.java) }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogDebugReportBinding

    /** Adapter for the report */
    private val reportAdapter = DebugReportAdapter(
        viewModel::collapseExpandEvent,
        viewModel::collapseExpandCondition,
    )

    override val emptyTextId: Int = R.string.dialog_debug_report_empty
    override fun onCreateListBinging() = viewBinding.layoutList

    override fun onCreateDialog(): BottomSheetDialog {
        viewBinding = DialogDebugReportBinding.inflate(LayoutInflater.from(context))

        return BottomSheetDialog(context).apply {
            //setCustomTitle(R.layout.view_dialog_title, R.string.dialog_debug_report_title)
            setContentView(viewBinding.root)
            //setPositiveButton(android.R.string.ok, null)
        }
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        super.onDialogCreated(dialog)

        listBinding.list.adapter = reportAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportItems.collect { reportItems ->
                    updateLayoutState(reportItems)
                    reportAdapter.submitList(reportItems)
                }
            }
        }
    }
}