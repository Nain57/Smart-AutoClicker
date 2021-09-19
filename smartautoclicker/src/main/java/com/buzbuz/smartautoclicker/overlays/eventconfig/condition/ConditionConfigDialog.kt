/*
 * Copyright (C) 2021 Nain57
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
package com.buzbuz.smartautoclicker.overlays.eventconfig.condition

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.SeekBar

import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.setCustomTitle
import com.buzbuz.smartautoclicker.baseui.overlays.OverlayDialogController
import com.buzbuz.smartautoclicker.database.domain.Condition
import com.buzbuz.smartautoclicker.databinding.DialogConditionConfigBinding

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * [OverlayDialogController] implementation for displaying an event condition and providing a button to delete it.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param condition the event condition that will be edited.
 * @param onConfirmClicked the listener called when the user presses the ok button.
 * @param onDeleteClicked the listener called when the user presses the delete button.
 */
class ConditionConfigDialog(
    context: Context,
    private val condition: Condition,
    private val onConfirmClicked: (Condition) -> Unit,
    private val onDeleteClicked: () -> Unit
) : OverlayDialogController(context) {

    /** The view model for this dialog. */
    private var viewModel: ConditionConfigModel? = ConditionConfigModel(context).apply {
        attachToLifecycle(this@ConditionConfigDialog)
        setConfigCondition(condition)
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConditionConfigBinding
    /** The coroutine job fetching asynchronously the condition bitmap. */
    private var bitmapLoadingJob: Job? = null

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogConditionConfigBinding.inflate(LayoutInflater.from(context))
        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_condition_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel?.let {
                    onConfirmClicked.invoke(it.getConfiguredCondition())
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.dialog_condition_delete) { _: DialogInterface, _: Int ->
                onDeleteClicked.invoke()
            }
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        condition.let { condition ->
            viewBinding.textAreaAt.text = context.getString(
                R.string.dialog_condition_at,
                condition.area.left,
                condition.area.top,
                condition.area.right,
                condition.area.bottom
            )
            viewBinding.seekbarDiffThreshold.apply {
                max = MAX_THRESHOLD
                progress = condition.threshold
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        viewModel?.setThreshold(progress)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            bitmapLoadingJob?.cancel()
            bitmapLoadingJob = viewModel?.getConditionBitmap(condition) { bitmap ->
                if (bitmap != null) {
                    viewBinding.imageCondition.setImageBitmap(bitmap)
                } else {
                    viewBinding.imageCondition.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_cancel)?.apply {
                            setTint(Color.RED)
                        }
                    )
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    viewBinding.textAreaAt.setText(R.string.dialog_condition_error)
                }
            }

            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel?.threshold?.collect { threshold ->
                        viewBinding.textDiffThreshold.text = context.getString(
                            R.string.dialog_condition_threshold_value,
                            threshold
                        )
                    }
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        bitmapLoadingJob?.cancel()
        viewModel = null
    }
}
