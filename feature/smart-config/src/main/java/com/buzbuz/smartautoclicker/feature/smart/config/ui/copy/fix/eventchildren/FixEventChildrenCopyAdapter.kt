/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.eventchildren

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemFixCopyChildBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemFixCopyMissingRefBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemMessageHeaderBinding
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixCopyUiItem

/**
 * Adapter displaying the list of conditions and actions to fix before copy.
 * @param onMissingReferenceClicked Called when the user clicks on a specific missing reference within an item.
 */
class FixEventChildrenCopyAdapter(
    private val onMissingReferenceClicked: (FixCopyUiItem.Item.EventChildren, MissingCopyReference) -> Unit,
) : ListAdapter<FixCopyUiItem, RecyclerView.ViewHolder>(FixEventChildrenDiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is FixCopyUiItem.Header -> R.layout.item_message_header
            is FixCopyUiItem.Item.EventChildren -> R.layout.item_fix_copy_child
            else -> throw IllegalArgumentException("Unsupported item type!")
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_message_header -> ChildrenHeaderViewHolder(
                ItemMessageHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_fix_copy_child -> EventChildViewHolder(
                ItemFixCopyChildBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unsupported view type!")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ChildrenHeaderViewHolder -> holder.onBind(getItem(position) as FixCopyUiItem.Header)
            is EventChildViewHolder -> holder.onBind(
                item = getItem(position) as FixCopyUiItem.Item.EventChildren,
                onMissingReferenceClicked = onMissingReferenceClicked,
            )
        }
    }
}

/** DiffUtil Callback comparing two [FixCopyUiItem] when updating the [FixEventChildrenCopyAdapter] list. */
private object FixEventChildrenDiffUtilCallback : DiffUtil.ItemCallback<FixCopyUiItem>() {
    override fun areItemsTheSame(oldItem: FixCopyUiItem, newItem: FixCopyUiItem): Boolean =
        when (oldItem) {
            is FixCopyUiItem.Header if newItem is FixCopyUiItem.Header -> true
            is FixCopyUiItem.Item.EventChildren.ActionItem if newItem is FixCopyUiItem.Item.EventChildren.ActionItem ->
                oldItem.uiAction.action.id == newItem.uiAction.action.id
            is FixCopyUiItem.Item.EventChildren.ConditionItem if newItem is FixCopyUiItem.Item.EventChildren.ConditionItem ->
                oldItem.uiCondition.condition.id == newItem.uiCondition.condition.id
            else -> false
        }

    override fun areContentsTheSame(oldItem: FixCopyUiItem, newItem: FixCopyUiItem): Boolean = oldItem == newItem
}

/**
 * View holder displaying a header in the [FixEventChildrenCopyAdapter].
 * @param viewBinding the view binding for this header.
 */
private class ChildrenHeaderViewHolder(
    private val viewBinding: ItemMessageHeaderBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(header: FixCopyUiItem.Header) {
        viewBinding.headerText.setText(header.message)
    }
}

/**
 * View holder displaying a condition or action item in the [FixEventChildrenCopyAdapter].
 * @param viewBinding the view binding for this item.
 */
private class EventChildViewHolder(
    private val viewBinding: ItemFixCopyChildBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(
        item: FixCopyUiItem.Item.EventChildren,
        onMissingReferenceClicked: (FixCopyUiItem.Item.EventChildren, MissingCopyReference) -> Unit,
    ) {
        when (item) {
            is FixCopyUiItem.Item.EventChildren.ActionItem -> bindAction(item)
            is FixCopyUiItem.Item.EventChildren.ConditionItem -> bindCondition(item)
        }

        viewBinding.iconChildCanBeCopied.setImageResource(
            if (item.isValidForCopy) R.drawable.ic_confirm else R.drawable.ic_warning
        )

        bindMissingReferences(item, item.itemWithMissingReferences.missingReferences, onMissingReferenceClicked)
    }

    private fun bindAction(item: FixCopyUiItem.Item.EventChildren.ActionItem) {
        viewBinding.iconChildType.setImageResource(item.uiAction.icon)
        viewBinding.childName.text = item.uiAction.name
        viewBinding.childDetails.text = item.stateText
    }

    private fun bindCondition(item: FixCopyUiItem.Item.EventChildren.ConditionItem) {
        viewBinding.iconChildType.setImageResource(item.uiCondition.iconRes)
        viewBinding.childName.text = item.uiCondition.name
        viewBinding.childDetails.text = item.stateText
    }

    private fun bindMissingReferences(
        item: FixCopyUiItem.Item.EventChildren,
        references: List<MissingCopyReference>,
        onMissingReferenceClicked: (FixCopyUiItem.Item.EventChildren, MissingCopyReference) -> Unit,
    ) {
        val container = viewBinding.listMissingReferences
        if (references.isEmpty()) {
            container.visibility = View.GONE
            return
        }

        container.visibility = View.VISIBLE
        container.removeAllViews()

        val inflater = LayoutInflater.from(container.context)
        references.forEach { reference ->
            val refBinding = ItemFixCopyMissingRefBinding.inflate(inflater, container, true)
            refBinding.iconMissingRefType.setImageResource(reference.getIconRes())
            refBinding.textMissingRefName.text = reference.name
            refBinding.root.setOnClickListener { onMissingReferenceClicked(item, reference) }
        }
    }
}

@DrawableRes
private fun MissingCopyReference.getIconRes(): Int =
    when (this) {
        is MissingCopyReference.EventToggleReference -> R.drawable.ic_toggle_event
        is MissingCopyReference.ScreenConditionReference -> R.drawable.ic_image_condition
        is MissingCopyReference.CounterReference -> R.drawable.ic_change_counter
    }
