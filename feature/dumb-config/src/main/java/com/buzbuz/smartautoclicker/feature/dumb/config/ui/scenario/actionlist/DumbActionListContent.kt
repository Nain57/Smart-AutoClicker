
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario.actionlist

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.NavBarDialogContent
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.navbar.viewModels
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.core.ui.databinding.IncludeLoadableListBinding
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.di.DumbConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.DumbActionCreator
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.DumbActionUiFlowListener
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.copy.DumbActionDetails
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.startDumbActionCreationUiFlow
import com.buzbuz.smartautoclicker.feature.dumb.config.ui.actions.startDumbActionEditionUiFlow

import kotlinx.coroutines.launch

class DumbActionListContent(appContext: Context) : NavBarDialogContent(appContext) {

    /** View model for the container dialog. */
    private val viewModel: DumbActionListViewModel by viewModels(
        entryPoint = DumbConfigViewModelsEntryPoint::class.java,
        creator = { dumbActionListViewModel() },
    )

    /** View binding for all views in this content. */
    private lateinit var viewBinding: IncludeLoadableListBinding
    /** Adapter for the list of actions. */
    private lateinit var dumbActionsAdapter: DumbActionListAdapter

    private lateinit var createCopyActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var updateActionUiFlowListener: DumbActionUiFlowListener
    private lateinit var dumbActionCreator: DumbActionCreator

    /** TouchHelper applied to [dumbActionsAdapter] allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(DumbActionReorderTouchHelper())

    override fun createCopyButtonsAreAvailable(): Boolean = true

    override fun onCreateView(container: ViewGroup): ViewGroup {
        dumbActionsAdapter = DumbActionListAdapter(
            actionClickedListener = ::onDumbActionClicked,
            actionReorderListener = viewModel::updateDumbActionOrder,
        )

        viewBinding = IncludeLoadableListBinding.inflate(LayoutInflater.from(context), container, false).apply {
            setEmptyText(
                id = R.string.message_empty_dumb_action_list,
                secondaryId = R.string.message_empty_secondary_dumb_action_list,
            )
            list.apply {
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                itemTouchHelper.attachToRecyclerView(this)
                adapter = dumbActionsAdapter
            }
        }

        return viewBinding.root
    }

    override fun onViewCreated() {
        dumbActionCreator = DumbActionCreator(
            createNewDumbClick = { position -> viewModel.createNewDumbClick(context, position) },
            createNewDumbSwipe = { from, to -> viewModel.createNewDumbSwipe(context, from, to) },
            createNewDumbPause = { viewModel.createNewDumbPause(context) },
            createDumbActionCopy = viewModel::createDumbActionCopy,
        )
        createCopyActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = viewModel::addNewDumbAction,
            onDumbActionDeleted = {},
            onDumbActionCreationCancelled = {},
        )
        updateActionUiFlowListener = DumbActionUiFlowListener(
            onDumbActionSaved = viewModel::updateDumbAction,
            onDumbActionDeleted = viewModel::deleteDumbAction,
            onDumbActionCreationCancelled = {},
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.dumbActionsDetails.collect(::updateDumbActionList) }
                launch { viewModel.canCopyAction.collect(::updateCopyButtonState) }
            }
        }
    }

    override fun onCreateButtonClicked() {
        debounceUserInteraction {
            dialogController.overlayManager.startDumbActionCreationUiFlow(
                context = context,
                creator = dumbActionCreator,
                listener = createCopyActionUiFlowListener,
            )
        }
    }

    override fun onCopyButtonClicked() {
        debounceUserInteraction {

        }
    }

    private fun onDumbActionClicked(dumbActionDetails: DumbActionDetails) {
        debounceUserInteraction {
            dialogController.overlayManager.startDumbActionEditionUiFlow(
                context = context,
                dumbAction = dumbActionDetails.action,
                listener = updateActionUiFlowListener,
            )
        }
    }

    private fun updateDumbActionList(newList: List<DumbActionDetails>) {
        viewBinding.updateState(newList)
        dumbActionsAdapter.submitList(newList)
    }

    private fun updateCopyButtonState(enabled: Boolean) {
        dialogController.createCopyButtons.buttonCopy.apply {
            if (enabled) show() else hide()
        }
    }
}