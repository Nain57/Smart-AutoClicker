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
package com.buzbuz.smartautoclicker.overlays.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.databinding.DialogMultiChoiceBinding
import com.buzbuz.smartautoclicker.databinding.ItemMultiChoiceBinding

/**
 * [OverlayDialogController] implementation for a dialog displaying a list of choices to the user.
 *
 * @param T the type of choices in the list.
 * @param context the Android Context for the dialog shown by this controller.
 * @param dialogTitle the title of the dialog.
 * @param choices the choices to be displayed.
 * @param onChoiceSelected the callback to be notified upon user choice selection.
 */
class MultiChoiceDialog<T : DialogChoice>(
    context: Context,
    private val dialogTitle: Int,
    choices: List<T>,
    private val onChoiceSelected: (T) -> Unit
) : OverlayDialogController(context) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogMultiChoiceBinding

    /** The adapter displaying the choices. */
    private val adapter = ChoiceAdapter(choices) { choice ->
        onChoiceSelected.invoke(choice)
        dismiss()
    }

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogMultiChoiceBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, dialogTitle)
            .setView(viewBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        viewBinding.list.adapter = adapter
    }
}

/**
 * Adapter displaying the choices in the dialog.
 *
 * @param T the type of choices in the list.
 * @param choices the choices to be displayed in the list.
 * @param onChoiceSelected called when the user clicks on a choice.
 */
private class ChoiceAdapter<T : DialogChoice>(
    private val choices: List<T>,
    private val onChoiceSelected: (T) -> Unit,
): RecyclerView.Adapter<ChoiceViewHolder>() {

    override fun getItemCount(): Int = choices.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoiceViewHolder =
        ChoiceViewHolder(ItemMultiChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ChoiceViewHolder, position: Int) {
        holder.holderViewBinding.textChoiceFirst.apply {
            val choice = choices[position]

            setOnClickListener { onChoiceSelected.invoke(choice) }

            setText(choice.title)
            choice.iconId?.let { icon ->
                setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, R.drawable.ic_chevron, 0)
            }
        }
    }
}

/**
 * View holder for a choice.
 * @param holderViewBinding the view binding containing the holder root view.
 */
private class ChoiceViewHolder(
    val holderViewBinding: ItemMultiChoiceBinding,
) : RecyclerView.ViewHolder(holderViewBinding.root)

/** Base class for a dialog choice. */
open class DialogChoice(
    val title: Int,
    @DrawableRes val iconId: Int?,
)