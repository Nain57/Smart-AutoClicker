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

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.ui.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.ui.overlays.menu.OverlayMenu
import com.buzbuz.smartautoclicker.core.ui.overlays.viewModels
import com.buzbuz.smartautoclicker.core.ui.utils.AutoHideAnimationController
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.databinding.OverlayDumbScenarioBriefMenuBinding
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.DumbActionCreator
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.DumbActionUiFlowListener
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionCopyUiFlow
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionCreationUiFlow
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions.startDumbActionEditionUiFlow
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.scenario.actionlist.DumbActionDetails

import kotlinx.coroutines.launch

class DumbScenarioBriefMenu(
    private val onConfigSaved: () -> Unit,
) : OverlayMenu(theme = R.style.DumbScenarioConfigTheme, recreateOverlayViewOnRotation = true) {

    /** The view model for this menu. */
    private val viewModel: DumbScenarioBriefViewModel by viewModels()

    private val actionListSnapHelper: PositionPagerSnapHelper = PositionPagerSnapHelper()

    /** The view binding for the overlay menu. */
    private lateinit var menuViewBinding: OverlayDumbScenarioBriefMenuBinding
    /** The view binding for the position selector. */
    private lateinit var visualisationViewBinding: DumbScenarioBriefViewBinding
    /** The adapter for the list of dumb actions. */
    private lateinit var dumbActionsAdapter: DumbActionBriefAdapter
    /** Controls the action brief panel in and out animations. */
    private lateinit var actionBriefPanelAnimationController: AutoHideAnimationController

    private lateinit var dumbActionCreator: DumbActionCreator
    private lateinit var createCopyActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var updateActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var recyclerViewLayoutManager: LinearLayoutManagerExt

    override fun onCreate() {
        super.onCreate()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.canCopyAction.collect(::onCopyMenuButtonStateUpdated) }
                launch { viewModel.visualizedActions.collect(::onDumbActionListUpdated) }
                launch { viewModel.focusedActionDetails.collect(::onFocusedActionDetailsUpdated) }
            }
        }
    }

    override fun onCreateMenu(layoutInflater: LayoutInflater): ViewGroup {
        actionBriefPanelAnimationController = AutoHideAnimationController()
        dumbActionsAdapter = DumbActionBriefAdapter(displayMetrics, ::onDumbActionCardClicked)

        dumbActionCreator = DumbActionCreator(
            createNewDumbClick = viewModel::createNewDumbClick,
            createNewDumbSwipe = viewModel::createNewDumbSwipe,
            createNewDumbPause = viewModel::createNewDumbPause,
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

    override fun onCreateOverlayView(): View {
        visualisationViewBinding = context.getSystemService(LayoutInflater::class.java)
            .inflateDumbScenarioBriefViewBinding(displayMetrics.orientation)
            .apply {

                actionBriefPanelAnimationController.attachToView(
                    layoutActionList,
                    if (displayMetrics.orientation == Configuration.ORIENTATION_PORTRAIT)
                        AutoHideAnimationController.ScreenSide.BOTTOM
                    else
                        AutoHideAnimationController.ScreenSide.LEFT
                )

                listDumbActions.adapter = dumbActionsAdapter
                recyclerViewLayoutManager = LinearLayoutManagerExt(context, displayMetrics.orientation).apply {
                    setNextLayoutCompletionListener {
                        actionListSnapHelper.snapTo(viewModel.actionListSnapIndex.value)
                    }
                }
                listDumbActions.layoutManager = recyclerViewLayoutManager

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

        return visualisationViewBinding.root
    }

    override fun onResume() {
        super.onResume()
        actionBriefPanelAnimationController.showOrResetTimer()
    }

    override fun onDestroy() {
        actionBriefPanelAnimationController.detachFromView()
        super.onDestroy()
    }

    override fun onScreenOverlayVisibilityChanged(isVisible: Boolean) {
        if (isVisible) actionBriefPanelAnimationController.showOrResetTimer()
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
            actionBriefPanelAnimationController.hide()

            OverlayManager.getInstance(context).startDumbActionCreationUiFlow(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener,
            )
        }
    }

    private fun onCopyDumbActionClicked() {
        debounceUserInteraction {
            actionBriefPanelAnimationController.hide()

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
        recyclerViewLayoutManager.setNextLayoutCompletionListener {
            actionListSnapHelper.snapTo(index)
        }

        viewModel.addNewDumbAction(action, index)
    }

    private fun onCopyMenuButtonStateUpdated(isEnabled: Boolean) {
        setMenuItemViewEnabled(
            view = menuViewBinding.btnCopy,
            enabled = isEnabled,
            clickable = isEnabled,
        )
    }

    private fun onDumbActionListUpdated(actions: List<DumbActionDetails>) {
        dumbActionsAdapter.submitList(actions)

        visualisationViewBinding.apply {
            if (actions.isEmpty()) {
                listDumbActions.visibility = View.GONE
                emptyScenarioCard.visibility = View.VISIBLE
            } else {
                listDumbActions.visibility = View.VISIBLE
                emptyScenarioCard.visibility = View.GONE
            }
        }
    }

    private fun onFocusedActionDetailsUpdated(details: FocusedActionDetails) {
        visualisationViewBinding.apply {
            if (details.isEmpty) {
                textDumbActionIndex.setText(R.string.item_title_no_dumb_actions)
                buttonPrevious.isEnabled = false
                buttonNext.isEnabled = false
            } else {
                textDumbActionIndex.text = context.getString(
                    R.string.title_action_count,
                    details.actionIndex + 1,
                    details.actionCount,
                )

                buttonPrevious.isEnabled = details.actionIndex != 0
                buttonNext.isEnabled = details.actionIndex != details.actionCount - 1
            }
            viewDumbBrief.setDescription(details.actionDescription)
        }
    }
}

private class LinearLayoutManagerExt(context: Context, screenOrientation: Int) : LinearLayoutManager(
    /* context */ context,
    /* orientation */ if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) HORIZONTAL else VERTICAL,
    /* reverseLayout */false,
) {

    private var nextLayoutCompletionListener: (() -> Unit)? = null

    fun setNextLayoutCompletionListener(listener: () -> Unit) {
        nextLayoutCompletionListener = listener
    }

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        if (nextLayoutCompletionListener != null) {
            nextLayoutCompletionListener?.invoke()
            nextLayoutCompletionListener = null
        }
    }
}