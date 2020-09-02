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
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.appcompat.app.AlertDialog

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.ui.base.DialogController

import kotlinx.android.synthetic.main.dialog_click_condition.image_condition
import kotlinx.android.synthetic.main.dialog_click_condition.text_area_1
import kotlinx.android.synthetic.main.dialog_click_condition.text_area_2

/**
 * [DialogController] implementation for displaying a click condition and providing a button to delete it.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param condition the click condition to be displayed.
 * @param onDeleteClicked the listener called when the user presses the delete button.
 */
class ClickConditionDialog(
    context: Context,
    private val condition: Pair<Pair<Rect, Bitmap>, Int>,
    private val onDeleteClicked: (Int) -> Unit
) : DialogController() {

    override val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(context)
        .setView(R.layout.dialog_click_condition)
        .setPositiveButton(android.R.string.ok, null)
        .setNegativeButton(R.string.dialog_click_condition_delete) { _: DialogInterface, _: Int -> onDeleteClicked.invoke(condition.second) }

    override val dialogTitle: Int = R.string.dialog_click_condition_title

    override fun onDialogShown(dialog: AlertDialog) {
        dialog.apply {
            condition.first.let {
                image_condition.setImageBitmap(it.second)
                text_area_1.text = context.getString(R.string.dialog_click_condition_area,
                    it.first.left, it.first.top)
                text_area_2.text = context.getString(R.string.dialog_click_condition_area,
                    it.first.right, it.first.bottom)
            }
        }
    }
}