/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.triggerevents

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.utils.setIconTintColor

/**
 * Adapter displaying a list of trigger events.
 * @param itemClickedListener listener called when the user clicks on an item.
 */
class TriggerEventListAdapter(
    private val itemClickedListener: (TriggerEvent) -> Unit,
) : ListAdapter<TriggerEvent, TriggerEventViewHolder>(TriggerEventDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TriggerEventViewHolder =
        TriggerEventViewHolder(ItemTriggerEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: TriggerEventViewHolder, position: Int) {
        holder.bindEvent(getItem(position), itemClickedListener)
    }
}

/** DiffUtil Callback comparing two ActionItem when updating the [TriggerEventListAdapter] list. */
object TriggerEventDiffUtilCallback: DiffUtil.ItemCallback<TriggerEvent>() {
    override fun areItemsTheSame(oldItem: TriggerEvent, newItem: TriggerEvent): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: TriggerEvent, newItem: TriggerEvent): Boolean =
        oldItem == newItem
}

/**
 * View holder displaying a click in the [TriggerEventListAdapter].
 * @param holderViewBinding the view binding for this item.
 */
class TriggerEventViewHolder(
    private val holderViewBinding: ItemTriggerEventBinding,
) : RecyclerView.ViewHolder(holderViewBinding.root) {

    /**
     * Bind this view holder to an event.
     *
     * @param item the item providing the binding data.
     * @param itemClickedListener listener called when an event is clicked.
     */
    fun bindEvent(item: TriggerEvent, itemClickedListener: (TriggerEvent) -> Unit) {
        holderViewBinding.apply {
            textName.text = item.name
            textConditionsCount.text = item.conditions.size.toString()
            textActionsCount.text = item.actions.size.toString()

            val typedValue = TypedValue()
            val actionColorAttr = if (!item.isComplete()) R.attr.colorError else R.attr.colorOnSurface
            root.context.theme.resolveAttribute(actionColorAttr, typedValue, true)
            textActionsCount.setTextColor(typedValue.data)
            imageAction.setIconTintColor(typedValue.data)

            if (item.enabledOnStart) {
                textEnabled.setText(R.string.dropdown_item_title_event_state_enabled)
                iconEnabled.setImageResource(R.drawable.ic_confirm)
            } else {
                textEnabled.setText(R.string.dropdown_item_title_event_state_disabled)
                iconEnabled.setImageResource(R.drawable.ic_cancel)
            }

            root.setOnClickListener { itemClickedListener(item) }
        }
    }
}
