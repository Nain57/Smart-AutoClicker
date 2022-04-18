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
package com.buzbuz.smartautoclicker.overlays.scenariosettings.endcondition

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.database.domain.EndCondition
import com.buzbuz.smartautoclicker.databinding.DialogEndConditionConfigBinding
import com.buzbuz.smartautoclicker.overlays.utils.OnAfterTextChangedListener

import kotlinx.coroutines.launch


/**
 * [OverlayDialogController] implementation for displaying the end condition configuration.
 **
 * @param context the Android Context for the dialog shown by this controller.
 * @param endCondition the end condition to be configured.
 * @param endConditions the complete list of events for this scenario.
 * @param onConfirmClicked called when the user clicks on confirm.
 * @param onDeleteClicked called when the user clicks on delete.
 */
class EndConditionConfigDialog(
    context: Context,
    endCondition: EndCondition,
    endConditions: List<EndCondition>,
    private val onConfirmClicked: (EndCondition) -> Unit,
    private val onDeleteClicked: () -> Unit
) : OverlayDialogController(context) {

    /** The view model for this dialog. */
    private var viewModel: EndConditionConfigModel? = EndConditionConfigModel(context).apply {
        attachToLifecycle(this@EndConditionConfigDialog)
        setEndCondition(endCondition, endConditions)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogEndConditionConfigBinding

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogEndConditionConfigBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_end_condition_config_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.dialog_condition_delete) { _, _ -> onDeleteClicked() }
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        viewBinding.apply {


            editExecutions.apply {
                setSelectAllOnFocus(true)
                filters = arrayOf(MaxInputFilter())
                addTextChangedListener(object : OnAfterTextChangedListener() {
                    override fun afterTextChanged(s: Editable?) {
                        viewModel?.setExecutions(viewBinding.editExecutions.text.toString().toInt())
                    }
                })
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel?.eventName?.collect { eventName ->
                        if (eventName == null) {
                            viewBinding.textEventName.setText(R.string.dialog_end_condition_config_event_no_selection)
                        } else {
                            viewBinding.textEventName.text = eventName
                        }
                    }
                }

                launch {
                    viewModel?.executions?.collect { executions ->
                        viewBinding.editExecutions.apply {
                            setText(executions.toString())
                            setSelection(text.length)
                        }
                    }
                }

                launch {
                    viewModel?.isValidEndCondition?.collect { isValid ->
                        changeButtonState(
                            button = dialog.getButton(AlertDialog.BUTTON_POSITIVE),
                            visibility = if (isValid) View.VISIBLE else View.INVISIBLE,
                            listener = { onOkClicked() }
                        )
                    }
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }

    private fun onOkClicked() {
        viewModel?.let { model ->
            onConfirmClicked(model.getConfiguredEndCondition())
        }
        dismiss()
    }
}

/** Input filter for an end condition execution. */
private class MaxInputFilter : InputFilter {

    override fun filter(
        source: CharSequence?,
        start: Int,
        end: Int,
        dest: Spanned?,
        dstart: Int,
        dend: Int
    ): CharSequence? {
        try {
            val input = (dest.toString() + source.toString()).toInt()
            if (input in 0..EXECUTIONS_MAX_COUNT) return null
        } catch (nfe: NumberFormatException) {
        }
        return ""
    }

}

/** Maximum number of executions. */
private const val EXECUTIONS_MAX_COUNT = 99