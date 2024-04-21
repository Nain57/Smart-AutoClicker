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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings

import android.util.TypedValue
import android.view.View

import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemImageEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.utils.setIconTintColor

/**
 * Bind the [ItemImageEventBinding] to an event.
 *
 * @param event the event represented by this view binding.
 * @param canDrag true to show the drag handle, false to hide it.
 * @param itemClickedListener called when the user clicks on the item.
 */
fun ItemImageEventBinding.bind(
    event: ImageEvent,
    canDrag: Boolean,
    itemClickedListener: (ImageEvent) -> Unit,
) {
    textName.text = event.name
    textConditionsCount.text = event.conditions.size.toString()
    textActionsCount.text = event.actions.size.toString()

    val typedValue = TypedValue()
    val actionColorAttr = if (!event.isComplete()) R.attr.colorError else R.attr.colorOnSurface
    root.context.theme.resolveAttribute(actionColorAttr, typedValue, true)
    textActionsCount.setTextColor(typedValue.data)
    imageAction.setIconTintColor(typedValue.data)

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