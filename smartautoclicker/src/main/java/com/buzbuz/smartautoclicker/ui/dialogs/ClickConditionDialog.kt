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
package com.buzbuz.smartautoclicker.ui.dialogs

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.extensions.setCustomTitle
import com.buzbuz.smartautoclicker.core.ui.OverlayDialogController
import com.buzbuz.smartautoclicker.clicks.BitmapManager
import com.buzbuz.smartautoclicker.clicks.ClickCondition

import kotlinx.android.synthetic.main.dialog_click_condition.image_condition
import kotlinx.android.synthetic.main.dialog_click_condition.text_area_1
import kotlinx.android.synthetic.main.dialog_click_condition.text_area_2
import kotlinx.android.synthetic.main.dialog_click_condition.text_area_at
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** The coroutine job fetching asynchronously the condition bitmap from the [BitmapManager]. */
    private var bitmapJob: Job? = null

    override fun onCreateDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_click_condition_title)
            .setView(R.layout.dialog_click_condition)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(R.string.dialog_click_condition_delete) { _: DialogInterface, _: Int ->
                onDeleteClicked.invoke(condition.second)
            }
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        dialog.apply {
            condition.first.let {
                text_area_1.text = context.getString(R.string.dialog_click_condition_area, it.area.left, it.area.top)
                text_area_2.text = context.getString(R.string.dialog_click_condition_area, it.area.right, it.area.bottom)

                bitmapJob = CoroutineScope(Dispatchers.IO).launch {
                    val conditionBitmap = BitmapManager.getInstance(context).loadBitmap(
                        it.path, it.area.width(), it.area.height())

                    withContext(Dispatchers.Main) {
                        if (conditionBitmap != null) {
                            image_condition.setImageBitmap(conditionBitmap)
                        } else {
                            image_condition.setImageDrawable(
                                ContextCompat.getDrawable(context, R.drawable.ic_cancel)?.apply {
                                    setTint(Color.RED)
                                }
                            )
                            getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                            text_area_1.setText(R.string.dialog_click_condition_error)
                            text_area_2.text = null
                            text_area_at.visibility = View.INVISIBLE
                        }

                        bitmapJob = null
                    }
                }
            }
        }
    }
}