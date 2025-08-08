
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager


internal class ItemBriefAdapter(
    private val displayConfigManager: DisplayConfigManager,
    private val viewHolderCreator: (parent: ViewGroup, orientation: Int) -> ItemBriefViewHolder<*>,
    private val itemBoundListener: (index: Int, itemView: View?) -> Unit,
    private val onItemClickedListener: (Int, ItemBrief) -> Unit,
) : ListAdapter<ItemBrief, ItemBriefViewHolder<*>>(ItemBriefDiffUtilCallback) {

    private var orientation: Int = displayConfigManager.displayConfig.orientation

    override fun getItemViewType(position: Int): Int = orientation

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemBriefViewHolder<*> =
        viewHolderCreator(parent, viewType)

    override fun onBindViewHolder(holder: ItemBriefViewHolder<*>, position: Int) {
        holder.onBind(getItem(position), onItemClickedListener)
        itemBoundListener(position, holder.itemView)
    }

    override fun onViewRecycled(holder: ItemBriefViewHolder<*>) {
        itemBoundListener(holder.bindingAdapterPosition, null)
        super.onViewRecycled(holder)
    }

    public override fun getItem(position: Int): ItemBrief = super.getItem(position)

    @SuppressLint("NotifyDataSetChanged") // Reload the whole list when the orientation is different
    override fun submitList(list: List<ItemBrief>?) {
        if (orientation != displayConfigManager.displayConfig.orientation) {
            orientation = displayConfigManager.displayConfig.orientation
            notifyDataSetChanged()
            return
        }

        super.submitList(list)
    }
}

internal object ItemBriefDiffUtilCallback: DiffUtil.ItemCallback<ItemBrief>() {
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

abstract class ItemBriefViewHolder<T: ViewBinding>(
    protected val viewBinding: T,
) : RecyclerView.ViewHolder(viewBinding.root) {

    abstract fun onBind(item: ItemBrief, itemClickedListener: (Int, ItemBrief) -> Unit)
}

data class ItemBrief(
    val id: Identifier,
    val data: Any,
)