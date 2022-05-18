/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.utils

import android.view.View
import android.widget.ImageView

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.databinding.ItemEventBinding
import com.buzbuz.smartautoclicker.overlays.eventlist.EDITION
import com.buzbuz.smartautoclicker.overlays.eventlist.Mode
import com.buzbuz.smartautoclicker.overlays.eventlist.REORDER

/**
 * Bind this view holder to an event.
 *
 * @param event the item providing the binding data.
 * @param mode the current ui mode.
 * @param itemClickedListener listener called when an event is clicked.
 * @param deleteClickedListener listener called when the delete button is clicked.
 */
fun ItemEventBinding.bindEvent(
    event: Event,
    @Mode mode: Int? = null,
    itemClickedListener: (Event) -> Unit,
    deleteClickedListener: ((Event) -> Unit)? = null,
) {

    name.text = event.name

    actionsLayout.removeAllViews()
    event.actions?.forEach { action ->
        View.inflate(root.context, R.layout.view_action_icon, actionsLayout)
        (actionsLayout.getChildAt(actionsLayout.childCount - 1) as ImageView)
            .setImageResource(action.getIconRes())
    }

    when (mode) {
        EDITION -> bindEdition(event, itemClickedListener, deleteClickedListener!!)
        REORDER -> bindReorder()
        null -> bindSelection(event, itemClickedListener)
    }
}

/**
 * Bind this view holder to an event in edition mode.
 *
 * @param event the item providing the binding data.
 * @param itemClickedListener listener called when an event is clicked.
 * @param deleteClickedListener listener called when the delete button is clicked.
 */
private fun ItemEventBinding.bindEdition(event: Event, itemClickedListener: (Event) -> Unit, deleteClickedListener: (Event) -> Unit) {
    root.setOnClickListener { itemClickedListener(event) }
    btnAction.apply {
        visibility = View.VISIBLE
        setImageResource(R.drawable.ic_cancel)
        setOnClickListener { deleteClickedListener(event) }
    }
}

/** Bind this view holder to an event in reorder mode. */
private fun ItemEventBinding.bindReorder() {
    root.setOnClickListener(null)
    btnAction.apply {
        visibility = View.VISIBLE
        setImageResource(R.drawable.ic_drag)
        setOnClickListener(null)
    }
}

/**
 * Bind this view holder to an event in selection mode (chevron icon).
 *
 * @param event the item providing the binding data.
 * @param itemClickedListener listener called when an event is clicked.
 */
private fun ItemEventBinding.bindSelection(event: Event, itemClickedListener: (Event) -> Unit) {
    root.setOnClickListener { itemClickedListener(event) }
    btnAction.setImageResource(R.drawable.ic_chevron)
}