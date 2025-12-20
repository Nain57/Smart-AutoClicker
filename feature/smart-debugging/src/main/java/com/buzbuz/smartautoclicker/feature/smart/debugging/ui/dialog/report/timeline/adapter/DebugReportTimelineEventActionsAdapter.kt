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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.DebugReportTimelineEventActionItem


class DebugReportTimelineEventActionsAdapter()
    : ListAdapter<DebugReportTimelineEventActionItem, ItemTimelineEventActionsViewHolder>(EventActionsItemDiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemTimelineEventActionsViewHolder =
        ItemTimelineEventActionsViewHolder(parent)

    override fun onBindViewHolder(holder: ItemTimelineEventActionsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object EventActionsItemDiffUtilCallback: DiffUtil.ItemCallback<DebugReportTimelineEventActionItem>() {
    override fun areItemsTheSame(
        oldItem: DebugReportTimelineEventActionItem,
        newItem: DebugReportTimelineEventActionItem,
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: DebugReportTimelineEventActionItem,
        newItem: DebugReportTimelineEventActionItem,
    ): Boolean = oldItem == newItem
}