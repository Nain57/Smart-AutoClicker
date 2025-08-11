
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.intent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemIntentExtraCardBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemNewCopyCardBinding

/**
 * Adapter displaying a list of intent extra.
 *
 * @param addExtraClickedListener called when the user press the add extra item.
 * @param extraClickedListener called when the user press an extra.
 */
class ExtrasAdapter(
    private val addExtraClickedListener: () -> Unit,
    private val extraClickedListener: (IntentExtra<out Any>) -> Unit,
) : ListAdapter<ExtraListItem, RecyclerView.ViewHolder>(ExtraDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is ExtraListItem.AddExtraItem -> R.layout.item_new_copy_card
            is ExtraListItem.ExtraItem -> R.layout.item_intent_extra_card
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_new_copy_card -> AddExtraViewHolder(
                ItemNewCopyCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                addExtraClickedListener,
            )

            R.layout.item_intent_extra_card -> ExtraItemViewHolder(
                ItemIntentExtraCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                extraClickedListener,
            )

            else -> throw IllegalArgumentException("Unsupported view type !")
        }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ExtraItemViewHolder -> holder.onBind(((getItem(position) as ExtraListItem.ExtraItem)))
            is AddExtraViewHolder -> { /* nothing to do */ }
        }
    }

}

/** DiffUtil Callback comparing two ExtraListItem when updating the [ExtrasAdapter] list. */
private object ExtraDiffUtilCallback: DiffUtil.ItemCallback<ExtraListItem>() {
    override fun areItemsTheSame(oldItem: ExtraListItem, newItem: ExtraListItem): Boolean = when {
        oldItem is ExtraListItem.AddExtraItem && newItem is ExtraListItem.AddExtraItem -> true
        oldItem is ExtraListItem.ExtraItem && newItem is ExtraListItem.ExtraItem ->
            oldItem.name == newItem.name
        else -> false
    }

    override fun areContentsTheSame(oldItem: ExtraListItem, newItem: ExtraListItem): Boolean = oldItem == newItem
}

/** View holder for the add extra item. */
private class AddExtraViewHolder(
    viewBinding: ItemNewCopyCardBinding,
    addActionClickedListener: () -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root) {

    init {
        viewBinding.newItem.setOnClickListener { addActionClickedListener() }
        viewBinding.copyItem.visibility = View.GONE
    }
}

/** View holder for an extra item. */
private class ExtraItemViewHolder(
    private val viewBinding: ItemIntentExtraCardBinding,
    private val onExtraClickedListener: (IntentExtra<out Any>) -> Unit,
) : RecyclerView.ViewHolder(viewBinding.root)  {

    fun onBind(extra: ExtraListItem.ExtraItem) {
        viewBinding.apply {
            textExtraName.text = extra.name
            textExtraValue.text = extra.value
            root.setOnClickListener { onExtraClickedListener(extra.extra) }
        }
    }
}