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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.feature.smart.debugging.databinding.ItemTimelineEventOccurrenceBinding
import com.buzbuz.smartautoclicker.feature.smart.debugging.ui.dialog.report.timeline.DebugReportTimelineEventOccurrenceItem

class ItemTimelineEventViewHolder private constructor(
    private val viewBinding: ItemTimelineEventOccurrenceBinding,
) : RecyclerView.ViewHolder(viewBinding.root) {

    constructor(parent: ViewGroup) : this(
        viewBinding = ItemTimelineEventOccurrenceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    private val actionsAdapter: DebugReportTimelineEventActionsAdapter =
        DebugReportTimelineEventActionsAdapter()

    init {
        viewBinding.actions.adapter = actionsAdapter
    }

    fun bind(
        item: DebugReportTimelineEventOccurrenceItem,
        onItemClicked: (DebugReportTimelineEventOccurrenceItem) -> Unit,
    ) {
        viewBinding.apply {
            root.setOnClickListener { onItemClicked(item) }
            eventNameText.text = item.eventName
            timeText.text = item.timeText
            occurrenceText.text = item.occurrenceText
            conditionsText.text = item.conditionsText
        }

        actionsAdapter.submitList(item.actions)
    }
}