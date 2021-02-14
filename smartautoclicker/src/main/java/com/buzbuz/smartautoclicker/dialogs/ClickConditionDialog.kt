/*
 * Copyright (C) 2020 Nain57
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
package com.buzbuz.smartautoclicker.dialogs

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.setCustomTitle
import com.buzbuz.smartautoclicker.baseui.overlays.OverlayDialogController
import com.buzbuz.smartautoclicker.model.BitmapManager
import com.buzbuz.smartautoclicker.database.ClickCondition
import com.buzbuz.smartautoclicker.databinding.DialogClickConditionBinding
import com.buzbuz.smartautoclicker.model.DetectorModel

import kotlinx.coroutines.Job

/**
 * [OverlayDialogController] implementation for displaying a click condition and providing a button to delete it.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param condition the click condition to be displayed.
 * @param onDeleteClicked the listener called when the user presses the delete button.
 */
class ClickConditionDialog(
    context: Context,
    private val condition: Pair<ClickCondition, Int>,
    private val onDeleteClicked: (Int) -> Unit
) : OverlayDialogController(context) {

    private companion object {
        /** */
        private const val MAX_THRESHOLD = 20
    }

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogClickConditionBinding
    /** The coroutine job fetching asynchronously the condition bitmap from the [BitmapManager]. */
    private var bitmapJob: Job? = null

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogClickConditionBinding.inflate(LayoutInflater.from(context))
        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_click_condition_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                condition.first.threshold = viewBinding.seekbarDiffThreshold.progress
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.dialog_click_condition_delete) { _: DialogInterface, _: Int ->
                onDeleteClicked.invoke(condition.second)
            }
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        condition.first.let { condition ->
            viewBinding.textAreaAt.text = context.getString(
                R.string.dialog_click_condition_at,
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
                        updateThresholdDisplay(progress)
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
            updateThresholdDisplay(condition.threshold)

            condition.bitmap?.let {
                viewBinding.imageCondition.setImageBitmap(it)
                return
            }

            bitmapJob = DetectorModel.get().getClickConditionBitmap(condition) { bitmap ->
                if (bitmap != null) {
                    viewBinding.imageCondition.setImageBitmap(bitmap)
                } else {
                    viewBinding.imageCondition.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_cancel)?.apply {
                            setTint(Color.RED)
                        }
                    )
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                    viewBinding.textAreaAt.setText(R.string.dialog_click_condition_error)
                }

                bitmapJob = null
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        bitmapJob?.cancel()
    }

    /**
     * Update the display of the difference threshold value.
     *
     * @param value the new value to be displayed.
     */
    private fun updateThresholdDisplay(value: Int) {
        viewBinding.textDiffThreshold.text = context.getString(
            R.string.dialog_click_condition_threshold_value,
            value
        )
    }
}