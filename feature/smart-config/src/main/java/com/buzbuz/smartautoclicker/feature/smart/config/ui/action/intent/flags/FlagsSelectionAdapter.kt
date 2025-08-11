
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent.flags

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemIntentFlagBinding

class FlagsSelectionAdapter(
    private val onFlagCheckClicked: (Int, Boolean) -> Unit,
    private val onFlagHelpClicked: (Uri) -> Unit,
) : ListAdapter<ItemFlag, ItemFlagViewHolder>(ItemFlagDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemFlagViewHolder =
        ItemFlagViewHolder(
            viewBinding = ItemIntentFlagBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onFlagCheckClicked = onFlagCheckClicked,
            onFlagHelpClicked = onFlagHelpClicked,
        )

    override fun onBindViewHolder(holder: ItemFlagViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [FlagsSelectionAdapter] list. */
object ItemFlagDiffUtilCallback: DiffUtil.ItemCallback<ItemFlag>() {
    override fun areItemsTheSame(oldItem: ItemFlag, newItem: ItemFlag): Boolean =
        oldItem.flag.value == newItem.flag.value
    override fun areContentsTheSame(oldItem: ItemFlag, newItem: ItemFlag): Boolean =
        oldItem == newItem
}

/**
 * View holder displaying an action in the [FlagsSelectionAdapter].
 * @param viewBinding the view binding for this item.
 */
class ItemFlagViewHolder(
    private val viewBinding: ItemIntentFlagBinding,
    private val onFlagCheckClicked: (Int, Boolean) -> Unit,
    private val onFlagHelpClicked: (Uri) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: ItemFlag) {
        viewBinding.apply {
            flagName.text = item.flag.displayName
            buttonState.isChecked = item.isSelected

            btnHelp.setOnClickListener { onFlagHelpClicked(item.flag.helpUri) }
            buttonState.setOnClickListener { onFlagCheckClicked(item.flag.value, !item.isSelected) }
        }
    }
}
