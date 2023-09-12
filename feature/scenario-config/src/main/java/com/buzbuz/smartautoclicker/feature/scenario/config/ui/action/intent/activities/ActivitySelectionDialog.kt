/*
 * Copyright (C) 2023 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.activities

import android.content.ComponentName
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.ui.bindings.updateState
import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.DialogBaseSelectionBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.databinding.ItemApplicationBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent.ActivityDisplayInfo
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.bind

import com.google.android.material.bottomsheet.BottomSheetDialog

import kotlinx.coroutines.launch

/**
 * [OverlayDialog] implementation for displaying a list of Android activities.
 *
 * @param onApplicationSelected called when the user clicks on an application.
 */
class ActivitySelectionDialog(
    private val onApplicationSelected: (ComponentName) -> Unit,
) : OverlayDialog(R.style.ScenarioConfigTheme) {

    /** The view model for this dialog. */
    private val viewModel: ActivitySelectionModel by viewModels()
    /** ViewBinding containing the views for this dialog. */
    private lateinit var viewBinding: DialogBaseSelectionBinding

    /** Handle the binding between the application list and the views displaying them. */
    private lateinit var activitiesAdapter: ApplicationAdapter

    override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseSelectionBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                dialogTitle.setText(R.string.dialog_overlay_title_application_selection)
                buttonDismiss.setOnClickListener { debounceUserInteraction { back() } }
            }

            activitiesAdapter = ApplicationAdapter { selectedComponentName ->
                debounceUserInteraction {
                    onApplicationSelected(selectedComponentName)
                    back()
                }
            }

            layoutLoadableList.list.apply {
                adapter = activitiesAdapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activities.collect(::updateActivityList)
            }
        }
    }

    private fun updateActivityList(activities: List<ActivityDisplayInfo>) {
        viewBinding.layoutLoadableList.updateState(activities)
        activitiesAdapter.submitList(activities)
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

/** DiffUtil Callback comparing two lists when updating the [ApplicationAdapter]. */
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
        viewBinding.bind(activity, onApplicationSelected)
    }
}