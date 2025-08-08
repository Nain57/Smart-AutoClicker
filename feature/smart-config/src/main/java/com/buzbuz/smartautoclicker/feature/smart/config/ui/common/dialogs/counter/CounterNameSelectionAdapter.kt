
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.dialogs.counter

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemCounterNameBinding

class CounterNameSelectionAdapter(
    private val onCounterNameSelected: (String) -> Unit,
): ListAdapter<String, CounterNameViewHolder>(CounterNameDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CounterNameViewHolder =
        CounterNameViewHolder(
            ItemCounterNameBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onCounterNameSelected,
        )

    override fun onBindViewHolder(holder: CounterNameViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

}

/** DiffUtil Callback comparing two counter names when updating the [CounterNameSelectionAdapter] list. */
object CounterNameDiffUtilCallback: DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}


/**
 * View holder displaying an counter name in the [CounterNameSelectionAdapter].
 * @param viewBinding the view binding for this item.
 */
class CounterNameViewHolder(
    private val viewBinding: ItemCounterNameBinding,
    private val onCounterNameSelected: (String) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: String) {
        viewBinding.textCounterName.text = item
        viewBinding.root.setOnClickListener { onCounterNameSelected(item) }
    }
}
