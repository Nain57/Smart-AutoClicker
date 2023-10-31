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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.brief

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.OverlayDumbScenarioBriefBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.OverlayDumbScenarioBriefMenuBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.DumbActionCreator
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.DumbActionUiFlowListener
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionCopyUiFlow
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionCreationUiFlow
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionEditionUiFlow
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.actionlist.DumbActionDetails
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.utils.AutoHideAnimationController
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.utils.PositionPagerSnapHelper

import kotlinx.coroutines.launch

class DumbScenarioBriefMenu(
    private val onConfigSaved: () -> Unit,
) : OverlayMenu(theme = R.style.DumbScenarioConfigTheme) {

    /** The view model for this menu. */
    private val viewModel: DumbScenarioBriefViewModel by viewModels()

    private val actionListSnapHelper: PositionPagerSnapHelper = PositionPagerSnapHelper()

    /** The view binding for the overlay menu. */
    private lateinit var menuViewBinding: OverlayDumbScenarioBriefMenuBinding
    /** The view binding for the position selector. */
    private lateinit var visualisationViewBinding: OverlayDumbScenarioBriefBinding
    /** The adapter for the list of dumb actions. */
    private lateinit var dumbActionsAdapter: DumbActionBriefAdapter
    /** Controls the action brief panel in and out animations. */
    private lateinit var actionBriefPanelAnimationController: AutoHideAnimationController

    private lateinit var dumbActionCreator: DumbActionCreator
    private lateinit var createCopyActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var updateActionUiFlowListener: DumbActionUiFlowListener

    /**
     * When adding a dumb action, we need to focus its new item once it is added to the list.
     * To do that, we keep the expected new item index and list size, and snap to this item once
     * a list with the correct values is received.
     */
    private var addedActionIndexes: Pair<Int, Int>? = null

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.visualizedActions.collect(::onDumbActionListUpdated) }
                launch { viewModel.focusedActionDetails.collect(::onFocusedActionDetailsUpdated) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        dumbActionsAdapter = DumbActionBriefAdapter(::onDumbActionCardClicked)

        dumbActionCreator = DumbActionCreator(
            createNewDumbClick = viewModel::createNewDumbClick,
            createNewDumbSwipe = viewModel::createNewDumbSwipe,
            createNewDumbPause = viewModel::createNewDumbPause,
            createDumbActionCopy = null,
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

        visualisationViewBinding = OverlayDumbScenarioBriefBinding.inflate(layoutInflater).apply {
            actionBriefPanelAnimationController = AutoHideAnimationController()
            actionBriefPanelAnimationController.attachToView(layoutActionList)

            listDumbActions.adapter = dumbActionsAdapter
            actionListSnapHelper.apply {
                onSnapPositionChangeListener = { snapIndex ->
                    viewModel.onNewActionListSnapIndex(snapIndex)
                    actionBriefPanelAnimationController.showOrResetTimer()
                }
                attachToRecyclerView(listDumbActions)
            }

            root.setOnClickListener {
                actionBriefPanelAnimationController.showOrResetTimer()
            }
            buttonPrevious.setOnClickListener {
                actionBriefPanelAnimationController.showOrResetTimer()
                actionListSnapHelper.snapToPrevious()
            }
            buttonNext.setOnClickListener {
                actionBriefPanelAnimationController.showOrResetTimer()
                actionListSnapHelper.snapToNext()
            }
        }

        menuViewBinding = OverlayDumbScenarioBriefMenuBinding.inflate(layoutInflater)
        return menuViewBinding.root
    }

    override fun onCreateOverlayView(): View =
        visualisationViewBinding.root

    override fun onDestroy() {
        actionBriefPanelAnimationController.detachFromView()
        super.onDestroy()
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
            OverlayManager.getInstance(context).startDumbActionCreationUiFlow(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener,
            )
        }
    }

    private fun onCopyDumbActionClicked() {
        debounceUserInteraction {
            OverlayManager.getInstance(context).startDumbActionCopyUiFlow(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener
            )
        }
    }

    private fun onDumbActionCardClicked(dumbAction: DumbActionDetails) {
        debounceUserInteraction {
            OverlayManager.getInstance(context).startDumbActionEditionUiFlow(
                context = context,
                dumbAction = dumbAction.action,
                listener = updateActionUiFlowListener,
            )
        }
    }

    private fun onNewDumbActionCreated(action: DumbAction) {
        val index = actionListSnapHelper.snapPosition + 1

        addedActionIndexes = index to dumbActionsAdapter.itemCount + 1
        viewModel.addNewDumbAction(action, index)
    }

    private fun onDumbActionListUpdated(actions: List<DumbActionDetails>) {
        dumbActionsAdapter.submitList(actions)

        visualisationViewBinding.apply {
            if (actions.size > 1) {
                buttonPrevious.visibility = View.VISIBLE
                buttonNext.visibility = View.VISIBLE
            } else {
                buttonPrevious.visibility = View.INVISIBLE
                buttonNext.visibility = View.INVISIBLE
            }

            if (actions.isEmpty()) {
                listDumbActions.visibility = View.GONE
                emptyScenarioCard.visibility = View.VISIBLE
            } else {
                listDumbActions.visibility = View.VISIBLE
                emptyScenarioCard.visibility = View.GONE

                addedActionIndexes?.let { (requestedIndex, requestedCount) ->
                    if (requestedCount == actions.size) {
                        addedActionIndexes = null
                        lifecycleScope.launch {
                            actionListSnapHelper.snapTo(requestedIndex)
                        }
                    }
                }
            }
        }
    }

    private fun onFocusedActionDetailsUpdated(details: FocusedActionDetails) {
        visualisationViewBinding.apply {
            if (details.isEmpty) {
                textDumbActionIndex.setText(R.string.item_title_no_dumb_actions)
            } else {
                textDumbActionIndex.text = context.getString(
                    R.string.title_action_count,
                    details.actionIndex + 1,
                    details.actionCount,
                )
            }
            viewDumbBrief.setDescription(details.actionDescription)
        }
    }
}

