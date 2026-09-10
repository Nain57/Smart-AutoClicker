/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.DebugReportTimelineEventActionItem


class DebugReportTimelineEventActionsAdapter
    : RecyclerView.Adapter<ItemTimelineEventActionsViewHolder>() {

    private var items: List<DebugReportTimelineEventActionItem> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTimelineEventActionsViewHolder =
        ItemTimelineEventActionsViewHolder(parent)

    override fun onBindViewHolder(holder: ItemTimelineEventActionsViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<DebugReportTimelineEventActionItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
