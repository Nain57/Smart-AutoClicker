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
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.utils.setIconTintColor

/**
 * Bind this view holder to an event.
 *
 * @param item the item providing the binding data.
 * @param itemClickedListener listener called when an event is clicked.
 */
fun ItemTriggerEventBinding.bind(item: TriggerEvent, itemClickedListener: (TriggerEvent) -> Unit) {
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