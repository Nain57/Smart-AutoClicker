
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.brief

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.common.overlays.base.viewModels
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.DialogNavigationButton
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setButtonVisibility
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.updateState
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.DialogSmartActionsLegacyBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemSmartActionLegacyBinding
import com.buzbuz.smartautoclicker.feature.smart.config.di.ScenarioConfigViewModelsEntryPoint
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection.ActionTypeSelectionDialog
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.UiAction

import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.util.Collections


class SmartActionsLegacyDialog : OverlayDialog(R.style.ScenarioConfigTheme) {


    /** View model for this content. */
    private val viewModel: SmartActionsBriefViewModel by viewModels(
        entryPoint = ScenarioConfigViewModelsEntryPoint::class.java,
        creator = { smartActionsBriefViewModel() }
    )

    /** TouchHelper applied to [actionAdapter] allowing to drag and drop the items. */
    private val itemTouchHelper = ItemTouchHelper(ActionReorderTouchHelper())

    private lateinit var viewBinding: DialogSmartActionsLegacyBinding
    private lateinit var actionAdapter: ActionAdapter

    override fun onCreateView(): ViewGroup {
        actionAdapter = ActionAdapter(
            actionClickedListener = ::onActionClicked,
            actionReorderListener = viewModel::updateActionOrder,
        )

        viewBinding = DialogSmartActionsLegacyBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                setButtonVisibility(DialogNavigationButton.SAVE, View.GONE)
                setButtonVisibility(DialogNavigationButton.DELETE, View.GONE)
                dialogTitle.setText(R.string.menu_item_title_actions)

                buttonDismiss.setDebouncedOnClickListener { back() }
            }

            buttonNew.setDebouncedOnClickListener { onCreateButtonClicked() }
            buttonCopy.setDebouncedOnClickListener { onCopyButtonClicked() }

            layoutLoadableList.apply {
                setEmptyText(
                    id = R.string.message_empty_action_list_title,
                    secondaryId = R.string.message_empty_action_list_desc,
                )

                list.apply {
                    itemTouchHelper.attachToRecyclerView(this)
                    addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
                    adapter = actionAdapter
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                }
            }
        }

        return viewBinding.root
    }

    override fun onDialogCreated(dialog: BottomSheetDialog) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.canCopyActions.collect(::updateCopyButtonVisibility) }
                launch { viewModel.actionBriefList.collect(::updateActionList) }
            }
        }
    }

    private fun onCreateButtonClicked() {
        overlayManager.navigateTo(
            context = context,
            newOverlay = ActionTypeSelectionDialog(
                choices = viewModel.actionTypeChoices.value,
                onChoiceSelectedListener = { choiceClicked ->
                    showActionConfigDialog(viewModel, viewModel.createAction(context, choiceClicked))
                },
            ),
        )
    }

    private fun onCopyButtonClicked() {
        showActionCopyDialog(viewModel)
    }

    private fun onActionClicked(item: ItemBrief) {
        debounceUserInteraction { showActionConfigDialog(viewModel, (item.data as UiAction).action) }
    }

    private fun updateCopyButtonVisibility(isVisible: Boolean) {
        viewBinding.buttonCopy.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    @Suppress("UNCHECKED_CAST")
    private fun updateActionList(newItems: List<ItemBrief>?) {
        viewBinding.layoutLoadableList.apply {
            updateState(newItems)
            (list.adapter as ListAdapter<ItemBrief, RecyclerView.ViewHolder>).submitList(newItems)
        }
    }
}

private class ActionAdapter(
    private val actionClickedListener: (ItemBrief) -> Unit,
    private val actionReorderListener: (List<ItemBrief>) -> Unit,
) : ListAdapter<ItemBrief, ActionItemBriefViewHolder>(ActionItemBriefDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActionItemBriefViewHolder =
        ActionItemBriefViewHolder(ItemSmartActionLegacyBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ActionItemBriefViewHolder, position: Int) {
        holder.onBind(getItem(position), actionClickedListener)
    }

    /**
     * Swap the position of two events in the list.
     *
     * @param from the position of the click to be moved.
     * @param to the new position of the click to be moved.
     */
    fun moveActions(from: Int, to: Int) {
        val newList = currentList.toMutableList()
        Collections.swap(newList, from, to)
        submitList(newList)
    }

    /** Notify for an item drag and drop completion. */
    fun notifyMoveFinished() {
        actionReorderListener(currentList)
    }
}

private object ActionItemBriefDiffUtilCallback: DiffUtil.ItemCallback<ItemBrief>() {
    override fun areItemsTheSame(
        oldItem: ItemBrief,
        newItem: ItemBrief,
    ): Boolean = oldItem.id == newItem.id

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(
        oldItem: ItemBrief,
        newItem: ItemBrief,
    ): Boolean = oldItem.data == newItem.data
}

private class ActionItemBriefViewHolder(
    private val viewBinding: ItemSmartActionLegacyBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: ItemBrief, itemClickedListener: (ItemBrief) -> Unit) {
        viewBinding.apply {
            root.setOnClickListener { itemClickedListener(item) }

            val details = item.data as UiAction
            itemIcon.setImageResource(details.icon)
            itemName.text = details.name
            itemDescription.text = details.description
            errorBadge.visibility = if (details.haveError) View.VISIBLE else View.GONE
        }
    }
}

private class ActionReorderTouchHelper
    : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    /** Tells if the user is currently dragging an item. */
    private var isDragging: Boolean = false

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        isDragging = true

        (recyclerView.adapter as ActionAdapter).moveActions(
            viewHolder.bindingAdapterPosition,
            target.bindingAdapterPosition
        )
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Nothing do to
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)

        if (isDragging) {
            (recyclerView.adapter as ActionAdapter).notifyMoveFinished()
            isDragging = false
        }
    }
}