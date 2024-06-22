/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.brief

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ListAdapter

import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.ActionBriefMenu
import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.databinding.OverlayDumbScenarioBriefMenuBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.DumbActionCreator
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.DumbActionUiFlowListener
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.startDumbActionCopyUiFlow
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.startDumbActionCreationUiFlow
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.startDumbActionEditionUiFlow
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.actionlist.DumbActionDetails

import kotlinx.coroutines.launch

class DumbScenarioBriefMenu(
    private val onConfigSaved: () -> Unit,
) : ActionBriefMenu(
    theme = R.style.AppTheme,
    noActionsStringRes = R.string.message_dumb_brief_empty_action_list,
) {

    /** The view model for this menu. */
    private val viewModel: DumbScenarioBriefViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbScenarioBriefViewModel() },
    )

    /** The view binding for the overlay menu. */
    private lateinit var menuViewBinding: OverlayDumbScenarioBriefMenuBinding
    /** The adapter for the list of dumb actions. */
    private lateinit var dumbActionsAdapter: DumbActionBriefAdapter

    private lateinit var dumbActionCreator: DumbActionCreator
    private lateinit var createCopyActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var updateActionUiFlowListener: DumbActionUiFlowListener

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.canCopyAction.collect(::onCopyMenuButtonStateUpdated) }
                launch { viewModel.visualizedActions.collect(::updateActionList) }
                launch { viewModel.focusedActionDetails.collect(::onFocusedActionDetailsUpdated) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        dumbActionCreator = DumbActionCreator(
            createNewDumbClick = { position -> viewModel.createNewDumbClick(context, position) },
            createNewDumbSwipe = { from, to -> viewModel.createNewDumbSwipe(context, from, to) },
            createNewDumbPause = { viewModel.createNewDumbPause(context) },
            createDumbActionCopy = viewModel::createDumbActionCopy,
        )
        createCopyActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = ::onNewDumbActionCreated,
            onDumbActionDeleted = {},
            onDumbActionCreationCancelled = {},
        )
        updateActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = viewModel::updateDumbAction,
            onDumbActionDeleted = viewModel::deleteDumbAction,
            onDumbActionCreationCancelled = {},
        )

        menuViewBinding = OverlayDumbScenarioBriefMenuBinding.inflate(layoutInflater)
        return menuViewBinding.root
    }

    override fun onCreateAdapter(): ListAdapter<DumbActionDetails, DumbActionBriefViewHolder> {
        dumbActionsAdapter = DumbActionBriefAdapter(displayMetrics, ::onDumbActionCardClicked)
        return dumbActionsAdapter
    }

    override fun onMoveItem(from: Int, to: Int) {

    }

    override fun onDeleteItem(index: Int) {

    }

    override fun onPlayItem(index: Int) {

    }

    override fun onMenuItemClicked(viewId: Int) {
        when (viewId) {
            R.id.btn_back -> onBackClicked()
            R.id.btn_add -> onCreateDumbActionClicked()
            R.id.btn_copy -> onCopyDumbActionClicked()
        }
    }

    private fun onBackClicked() {
        debounceUserInteraction {
            onConfigSaved()
            back()
        }
    }

    private fun onCreateDumbActionClicked() {
        debounceUserInteraction {
            hidePanel()
            overlayManager.startDumbActionCreationUiFlow(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener,
            )
        }
    }

    private fun onCopyDumbActionClicked() {
        debounceUserInteraction {
            hidePanel()
            overlayManager.startDumbActionCopyUiFlow(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener
            )
        }
    }

    private fun onDumbActionCardClicked(dumbAction: DumbActionDetails) {
        debounceUserInteraction {
            overlayManager.startDumbActionEditionUiFlow(
                context = context,
                dumbAction = dumbAction.action,
                listener = updateActionUiFlowListener,
            )
        }
    }

    private fun onNewDumbActionCreated(action: DumbAction) {
        prepareItemInsertion()
        viewModel.addNewDumbAction(action, getFocusedItemIndex() + 1)
    }

    private fun onCopyMenuButtonStateUpdated(isEnabled: Boolean) {
        setMenuItemViewEnabled(
            view = menuViewBinding.btnCopy,
            enabled = isEnabled,
            clickable = isEnabled,
        )
    }

    override fun onFocusedItemChanged(index: Int) {
        super.onFocusedItemChanged(index)
        viewModel.onNewActionListSnapIndex(index)
    }

    private fun onFocusedActionDetailsUpdated(details: FocusedActionDetails) {
        briefViewBinding.apply {
            if (details.isEmpty) {
                textActionIndex.setText(R.string.item_title_no_dumb_actions)
            } else {
                textActionIndex.text = context.getString(
                    R.string.title_action_count,
                    details.actionIndex + 1,
                    details.actionCount,
                )
            }
            viewBrief.setDescription(details.actionDescription)
        }
    }
}
