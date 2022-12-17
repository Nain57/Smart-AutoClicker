/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.base.bindings

import android.view.View

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.databinding.ItemEventBinding
import com.buzbuz.smartautoclicker.domain.Event

/**
 * Bind the [ItemEventBinding] to an event.
 *
 * @param event the event represented by this view binding.
 *
 * @param canDrag true to show the drag handle, false to hide it.
 * @param itemClickedListener called when the user clicks on the item.
 */
fun ItemEventBinding.bind(event: Event, canDrag: Boolean, itemClickedListener: (Event) -> Unit) {
    textName.text = event.name
    textConditionsCount.text = event.conditions?.size?.toString()
    textActionsCount.text = event.actions?.size?.toString()

    if (event.enabledOnStart) {
        textEnabled.setText(R.string.dropdown_item_title_event_state_enabled)
        iconEnabled.setImageResource(R.drawable.ic_confirm)
    } else {
        textEnabled.setText(R.string.dropdown_item_title_event_state_disabled)
        iconEnabled.setImageResource(R.drawable.ic_cancel)
    }

    root.setOnClickListener { itemClickedListener(event) }

    btnReorder.visibility = if (canDrag) View.VISIBLE else View.GONE
}