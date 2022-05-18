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
package com.buzbuz.smartautoclicker.overlays.copy.conditions

import android.content.Context
import android.view.LayoutInflater
import android.view.View

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.domain.Condition
import com.buzbuz.smartautoclicker.databinding.DialogConditionCopyBinding
import com.buzbuz.smartautoclicker.overlays.utils.LoadableListDialog

import kotlinx.coroutines.launch

/**
 * [LoadableListDialog] implementation for displaying the whole list of conditions for a copy.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param conditions the list of edited conditions for the configured event.
 * @param onConditionSelected the listener called when the user select a Condition.
 */
class ConditionCopyDialog(
    context: Context,
    conditions: List<Condition>,
    private val onConditionSelected: (Condition) -> Unit,
) : LoadableListDialog(context)  {

    /** The view model for this dialog. */
    private var viewModel: ConditionCopyModel? = ConditionCopyModel(context).apply {
            attachToLifecycle(this@ConditionCopyDialog)
            setCurrentEventConditions(conditions)
        }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogConditionCopyBinding
    /** Adapter displaying the list of conditions. */
    private lateinit var conditionAdapter: ConditionCopyAdapter

    override val emptyTextId: Int = R.string.dialog_condition_copy_empty

    override fun getListBindingRoot(): View = viewBinding.root

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogConditionCopyBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_copy_title)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)

        conditionAdapter = ConditionCopyAdapter(
            conditionClickedListener = { selectedCondition ->
                viewModel?.let {
                    onConditionSelected(it.getNewConditionForCopy(selectedCondition))
                    dismiss()
                }
            },
            bitmapProvider = { bitmap, onLoaded ->
                viewModel?.getConditionBitmap(bitmap, onLoaded)
            },
        )

        listBinding.list.apply {
            adapter = conditionAdapter
            layoutManager = GridLayoutManager(
                context,
                2,
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.conditionList?.collect { actionList ->
                    updateLayoutState(actionList)
                    conditionAdapter.submitList(if (actionList == null) ArrayList() else ArrayList(actionList))
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }
}