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
package com.buzbuz.smartautoclicker.overlays.event.actions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ContentActionsBinding

import com.buzbuz.smartautoclicker.databinding.ContentConditionsBinding
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.overlays.base.NavBarDialogContent
import com.buzbuz.smartautoclicker.overlays.eventconfig.SubOverlay
import com.buzbuz.smartautoclicker.overlays.utils.LoadableListController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class ActionsContent(private val event: MutableStateFlow<Event?>) : NavBarDialogContent() {

    /** View model for this content. */
    private val viewModel: ActionsViewModel by lazy { ViewModelProvider(this).get(ActionsViewModel::class.java) }

    /** View binding for all views in this content. */
    private lateinit var viewBinding: ContentActionsBinding
    /** Controls the display state of the action list (empty, loading, loaded). */
    private lateinit var listController: LoadableListController<ActionListItem, RecyclerView.ViewHolder>
    /** Adapter for the list of actions. */
    private lateinit var actionAdapter: ActionAdapter

    override fun onCreateView(container: ViewGroup): ViewGroup {
        viewModel.setConfiguredEvent(event)

        viewBinding = ContentActionsBinding.inflate(LayoutInflater.from(context), container, false).apply {
            buttonNew.setOnClickListener { onNewButtonClicked() }
            buttonCopy.setOnClickListener { onCopyButtonClicked() }
        }

        actionAdapter = ActionAdapter(
            addActionClickedListener = {
                //subOverlayViewModel?.requestSubOverlay(SubOverlay.ActionTypeSelection)
            },
            copyActionClickedListener = {
                //subOverlayViewModel?.requestSubOverlay(SubOverlay.ActionCopy)
            },
            actionClickedListener = { index, action ->
               // subOverlayViewModel?.requestSubOverlay(SubOverlay.ActionConfig(action, index))
            },
            actionReorderListener = viewModel::updateActionOrder
        )

        listController = LoadableListController(
            owner = this,
            root = viewBinding.layoutList,
            adapter = actionAdapter,
            emptyTextId = R.string.dialog_conditions_empty,
        )
        listController.listView.adapter = actionAdapter

        return viewBinding.root
    }

    override fun onViewCreated() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.actionListItems.collect(listController::submitList) }
            }
        }
    }

    private fun onNewButtonClicked() {

    }

    private fun onCopyButtonClicked() {

    }
}