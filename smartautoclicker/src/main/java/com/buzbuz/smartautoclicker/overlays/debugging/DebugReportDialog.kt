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

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.databinding.DialogDebugReportBinding
import com.buzbuz.smartautoclicker.overlays.utils.LoadableListDialog
import kotlinx.coroutines.launch

class DebugReportDialog(context: Context): LoadableListDialog(context) {

    /** The view model for this dialog. */
    private var viewModel: DebugReportModel? = DebugReportModel(context).apply {
        attachToLifecycle(this@DebugReportDialog)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogDebugReportBinding

    /** Adapter for the report */
    private val reportAdapter = DebugReportAdapter()

    override val emptyTextId: Int = R.string.dialog_debug_report_empty
    override fun getListBindingRoot(): View = viewBinding.root

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogDebugReportBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_debug_report_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)

        listBinding.list.adapter = reportAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.reportItems?.collect { reportItems ->
                    updateLayoutState(reportItems)
                    reportAdapter.submitList(reportItems)
                }
            }
        }
    }
}