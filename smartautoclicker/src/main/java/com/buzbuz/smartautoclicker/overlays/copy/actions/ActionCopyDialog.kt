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
package com.buzbuz.smartautoclicker.overlays.copy.actions

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.databinding.DialogActionCopyBinding
import com.buzbuz.smartautoclicker.overlays.utils.LoadableListDialog
import com.buzbuz.smartautoclicker.overlays.utils.setIconTint

import kotlinx.coroutines.launch

/**
 * [LoadableListDialog] implementation for displaying the whole list of actions for a copy.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param actions the list of edited actions for the configured event.
 * @param onActionSelected the listener called when the user select an Action.
 */
class ActionCopyDialog(
    context: Context,
    actions: List<Action>,
    private val onActionSelected: (Action) -> Unit,
) : LoadableListDialog(context) {

    /** The view model for this dialog. */
    private var viewModel: ActionCopyModel? = ActionCopyModel(context).apply {
        attachToLifecycle(this@ActionCopyDialog)
        setCurrentEventActions(actions)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogActionCopyBinding
    /** Adapter displaying the list of events. */
    private lateinit var actionCopyAdapter: ActionCopyAdapter

    override val emptyTextId: Int = R.string.dialog_action_copy_empty

    override fun getListBindingRoot(): View = viewBinding.root

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogActionCopyBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(null)
            .setView(viewBinding.root)
            .setPositiveButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)

        viewBinding.search.apply {
            findViewById<ImageView>(androidx.appcompat.R.id.search_button).setIconTint(R.color.overlayViewPrimary)
            findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn).setIconTint(R.color.overlayViewPrimary)

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel?.updateSearchQuery(newText)
                    return true
                }
            })
        }

        actionCopyAdapter = ActionCopyAdapter { selectedAction ->
            viewModel?.let {
                onActionSelected(it.getNewActionForCopy(selectedAction))
                dismiss()
            }
        }

        listBinding.list.apply {
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            adapter = actionCopyAdapter
        }
        
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.actionList?.collect { actionList ->
                    updateLayoutState(actionList)
                    actionCopyAdapter.submitList(if (actionList == null) ArrayList() else ArrayList(actionList))
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }
}