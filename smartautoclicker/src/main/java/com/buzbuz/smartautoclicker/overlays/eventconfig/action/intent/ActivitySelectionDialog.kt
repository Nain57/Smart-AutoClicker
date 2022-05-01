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

import android.content.ComponentName
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.OverlayDialogController
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.databinding.DialogActivitySelectionBinding
import com.buzbuz.smartautoclicker.databinding.ItemApplicationBinding
import com.buzbuz.smartautoclicker.overlays.scenariosettings.EndConditionAdapter
import com.buzbuz.smartautoclicker.overlays.utils.LoadableListDialog

import kotlinx.coroutines.launch

/**
 * [OverlayDialogController] implementation for displaying a list of Android activities.
 *
 * @param context the Android Context for the dialog shown by this controller.
 * @param onApplicationSelected called when the user clicks on an application.
 */
class ActivitySelectionDialog(
    context: Context,
    private val onApplicationSelected: (ComponentName) -> Unit,
) : LoadableListDialog(context) {

    /** The view model for this dialog. */
    private var viewModel: ActivitySelectionModel? = ActivitySelectionModel(context).apply {
        attachToLifecycle(this@ActivitySelectionDialog)
    }
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogActivitySelectionBinding

    /** Handle the binding between the application list and the views displaying them. */
    private val adapter = ApplicationAdapter { selectedComponentName ->
        onApplicationSelected(selectedComponentName)
        dismiss()
    }

    override val emptyTextId: Int = R.string.dialog_application_select_empty

    override fun getListBindingRoot(): View = viewBinding.root

    override fun onCreateDialog(): AlertDialog.Builder {
        viewBinding = DialogActivitySelectionBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(context)
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_application_select_title)
            .setView(viewBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
    }

    override fun onDialogCreated(dialog: AlertDialog) {
        super.onDialogCreated(dialog)

        listBinding.list.adapter = adapter
        listBinding.list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel?.activities?.collect { activityList ->
                    updateLayoutState(activityList)
                    adapter.submitList(activityList)
                }
            }
        }
    }

    override fun onDialogDismissed() {
        super.onDialogDismissed()
        viewModel = null
    }
}

/**
 * Adapter for the list of applications.
 * @param onApplicationSelected listener on user click on an application.
 */
private class ApplicationAdapter(
    private val onApplicationSelected: (ComponentName) -> Unit,
) : ListAdapter<ActivityDisplayInfo, ApplicationViewHolder>(ApplicationDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder =
        ApplicationViewHolder(
            ItemApplicationBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onApplicationSelected,
        )

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) =
        holder.onBind(getItem(position))
}

/** DiffUtil Callback comparing two EndConditionListItem when updating the [EndConditionAdapter] list. */
private object ApplicationDiffUtilCallback: DiffUtil.ItemCallback<ActivityDisplayInfo>() {
    override fun areItemsTheSame(oldItem: ActivityDisplayInfo, newItem: ActivityDisplayInfo):
            Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: ActivityDisplayInfo, newItem: ActivityDisplayInfo):
            Boolean = oldItem == newItem
}

/**
 * ViewHolder for an application.
 *
 * @param viewBinding the view binding for this view holder views.
 * @param onApplicationSelected called when the user select an application.
 */
private class ApplicationViewHolder(
    private val viewBinding: ItemApplicationBinding,
    private val onApplicationSelected: (ComponentName) -> Unit,
): RecyclerView.ViewHolder(viewBinding.root) {

    /** Binds this view holder views to the provided activity. */
    fun onBind(activity: ActivityDisplayInfo) {
        viewBinding.apply {
            textApp.text = activity.name
            iconApp.setImageDrawable(activity.icon)

            root.setOnClickListener { onApplicationSelected(activity.componentName) }
        }
    }
}