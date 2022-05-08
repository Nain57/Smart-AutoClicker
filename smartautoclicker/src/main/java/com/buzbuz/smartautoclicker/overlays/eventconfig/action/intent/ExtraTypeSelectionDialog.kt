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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.intent

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.databinding.DialogIntentExtraTypeBinding
import com.buzbuz.smartautoclicker.databinding.ItemIntentExtraTypeBinding

import kotlin.reflect.KClass

/**
 * [OverlayDialogController] implementation for displaying a list of types for an intent extra.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param onTypeSelected called when the user clicks on a type.
 */
class ExtraTypeSelectionDialog(
    context: Context,
    private val onTypeSelected: (KClass<out Any>) -> Unit,
): OverlayDialogController(context) {

    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogIntentExtraTypeBinding

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogIntentExtraTypeBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_action_config_intent_advanced_extras_config_value_type)
            .setView(viewBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        viewBinding.typeList.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = ExtraTypeAdapter { selectedType ->
                onTypeSelected(selectedType)
                dismiss()
            }
        }
    }
}

/**
 * Adapter for the list of types.
 * @param onTypeSelected listener on user click on a type.
 */
private class ExtraTypeAdapter(
    private val onTypeSelected: (KClass<out Any>) -> Unit,
) : RecyclerView.Adapter<ExtraTypeViewHolder>() {

    /** The list of supported types for the extra value. */
    private val extraTypes = listOf(
        Boolean::class, Byte::class, Char::class, Double::class, Int::class, Float::class, Short::class, String::class,
    )

    override fun getItemCount(): Int = extraTypes.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExtraTypeViewHolder =
        ExtraTypeViewHolder(
            ItemIntentExtraTypeBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onTypeSelected,
        )

    override fun onBindViewHolder(holder: ExtraTypeViewHolder, position: Int) =
        holder.onBind(extraTypes[position])
}

/**
 * ViewHolder for an extra type.
 *
 * @param viewBinding the view binding for this view holder views.
 * @param onTypeSelected called when the user select a type.
 */
private class ExtraTypeViewHolder(
    private val viewBinding: ItemIntentExtraTypeBinding,
    private val onTypeSelected: (KClass<out Any>) -> Unit,
): RecyclerView.ViewHolder(viewBinding.root) {

    /** Binds this view holder views to the provided type. */
    fun onBind(type: KClass<out Any>) {
        viewBinding.textType.apply {
            text = type.simpleName
            setOnClickListener { onTypeSelected(type) }
        }
    }
}