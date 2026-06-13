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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.event

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemMessageHeaderBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemScreenEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiTriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.bind
import com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix.FixCopyUiItem

/**
 * Adapter displaying the list of events to fix before copy.
 * @param onEventClicked Called when the user clicks on an event item.
 */
class FixEventsCopyAdapter(
    private val onEventClicked: (FixCopyUiItem.Item.EventItem) -> Unit,
) : ListAdapter<FixCopyUiItem, RecyclerView.ViewHolder>(DiffUtilCallback) {

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is FixCopyUiItem.Header -> R.layout.item_message_header
            is FixCopyUiItem.Item.EventItem -> when ((getItem(position) as FixCopyUiItem.Item.EventItem).uiEvent) {
                is UiImageEvent -> R.layout.item_screen_event
                is UiTriggerEvent -> R.layout.item_trigger_event
            }
            else -> throw IllegalArgumentException("Unsupported item type!")
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.item_message_header -> HeaderViewHolder(
                ItemMessageHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_screen_event -> ScreenEventViewHolder(
                ItemScreenEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            R.layout.item_trigger_event -> TriggerEventViewHolder(
                ItemTriggerEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))
            else -> throw IllegalArgumentException("Unsupported view type!")
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.onBind(getItem(position) as FixCopyUiItem.Header)
            is ScreenEventViewHolder -> holder.onBind(
                item = getItem(position) as FixCopyUiItem.Item.EventItem,
                onEventClicked = onEventClicked,
            )
            is TriggerEventViewHolder -> holder.onBind(
                item = getItem(position) as FixCopyUiItem.Item.EventItem,
                onEventClicked = onEventClicked,
            )
        }
    }
}

/** DiffUtil Callback comparing two [FixCopyUiItem] when updating the [FixEventsCopyAdapter] list. */
private object DiffUtilCallback : DiffUtil.ItemCallback<FixCopyUiItem>() {
    override fun areItemsTheSame(oldItem: FixCopyUiItem, newItem: FixCopyUiItem): Boolean =
        when (oldItem) {
            is FixCopyUiItem.Header if newItem is FixCopyUiItem.Header -> true
            is FixCopyUiItem.Item.EventItem if newItem is FixCopyUiItem.Item.EventItem ->
                oldItem.uiEvent.event.id == newItem.uiEvent.event.id
            else -> false
        }

    override fun areContentsTheSame(oldItem: FixCopyUiItem, newItem: FixCopyUiItem): Boolean = oldItem == newItem
}

/**
 * View holder displaying a header in the [FixEventsCopyAdapter].
 * @param viewBinding the view binding for this header.
 */
private class HeaderViewHolder(
    private val viewBinding: ItemMessageHeaderBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(header: FixCopyUiItem.Header) {
        viewBinding.headerText.setText(header.message)
    }
}

/**
 * View holder displaying a screen event item in the [FixEventsCopyAdapter].
 * @param viewBinding the view binding for this item.
 */
private class ScreenEventViewHolder(
    private val viewBinding: ItemScreenEventBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: FixCopyUiItem.Item.EventItem, onEventClicked: (FixCopyUiItem.Item.EventItem) -> Unit) {
        viewBinding.bind(
            item = item.uiEvent as UiImageEvent,
            startBtnVisible = false,
            copyValidity = item.isValidForCopy,
            itemClickedListener = { onEventClicked(item) },
        )
    }
}

/**
 * View holder displaying a trigger event item in the [FixEventsCopyAdapter].
 * @param viewBinding the view binding for this item.
 */
private class TriggerEventViewHolder(
    private val viewBinding: ItemTriggerEventBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    fun onBind(item: FixCopyUiItem.Item.EventItem, onEventClicked: (FixCopyUiItem.Item.EventItem) -> Unit) {
        viewBinding.bind(
            item = item.uiEvent as UiTriggerEvent,
            copyValidity = item.isValidForCopy,
            itemClickedListener = { onEventClicked(item) },
        )
    }
}
