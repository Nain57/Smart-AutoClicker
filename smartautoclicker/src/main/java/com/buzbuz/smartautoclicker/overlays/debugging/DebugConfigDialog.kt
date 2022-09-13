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

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.databinding.DialogDebugConfigBinding
import com.buzbuz.smartautoclicker.extensions.setRightCompoundDrawable

import kotlinx.coroutines.launch

class DebugConfigDialog(context: Context): OverlayDialogController(context) {

    /** The view model for this dialog. */
    private var viewModel: DebugConfigModel? = DebugConfigModel(context).apply {
        attachToLifecycle(this@DebugConfigDialog)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogDebugConfigBinding

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogDebugConfigBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_debug_config_title)
            .setView(viewBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel?.saveConfig()
            }
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        viewBinding.debugViewToggle.setOnClickListener { viewModel?.toggleIsDebugViewEnabled() }
        viewBinding.debugReportToggle.setOnClickListener { viewModel?.toggleIsDebugReportEnabled() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel?.isDebugViewEnabled?.collect { debugViewEnabled ->
                       viewBinding.debugViewToggle.isChecked = debugViewEnabled
                    }
                }

                launch {
                    viewModel?.isDebugReportEnabled?.collect { debugReportEnabled ->
                        viewBinding.debugReportToggle.isChecked = debugReportEnabled
                    }
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }
}