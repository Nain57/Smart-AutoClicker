
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.intent

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemIntentActionBinding

class IntentActionsSelectionAdapter(
    private val onActionCheckClicked: (String, Boolean) -> Unit,
    private val onActionHelpClicked: (Uri) -> Unit,
) : ListAdapter<ItemAction, ItemIntentActionViewHolder>(ItemIntentActionDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemIntentActionViewHolder =
        ItemIntentActionViewHolder(
            viewBinding = ItemIntentActionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onActionCheckClicked = onActionCheckClicked,
            onActionHelpClicked = onActionHelpClicked,
        )

    override fun onBindViewHolder(holder: ItemIntentActionViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [IntentActionsSelectionAdapter] list. */
object ItemIntentActionDiffUtilCallback: DiffUtil.ItemCallback<ItemAction>() {
    override fun areItemsTheSame(oldItem: ItemAction, newItem: ItemAction): Boolean =
        oldItem.action.value == newItem.action.value
    override fun areContentsTheSame(oldItem: ItemAction, newItem: ItemAction): Boolean =
        oldItem == newItem
}

/**
 * View holder displaying an action in the [IntentActionsSelectionAdapter].
 * @param viewBinding the view binding for this item.
 */
class ItemIntentActionViewHolder(
    private val viewBinding: ItemIntentActionBinding,
    private val onActionCheckClicked: (String, Boolean) -> Unit,
    private val onActionHelpClicked: (Uri) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: ItemAction) {
        viewBinding.apply {
            actionName.text = item.action.displayName
            buttonState.isChecked = item.isSelected

            btnHelp.setOnClickListener { onActionHelpClicked(item.action.helpUri) }
            buttonState.setOnClickListener { onActionCheckClicked(item.action.value, !item.isSelected) }
        }
    }
}
