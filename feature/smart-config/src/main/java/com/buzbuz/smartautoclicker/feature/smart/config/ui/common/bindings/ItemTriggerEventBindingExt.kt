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

import com.buzbuz.smartautoclicker.core.base.extensions.getThemeColor
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.ItemTriggerEventBinding
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiTriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.utils.setIconTintColor

/**
 * Bind this view holder to an event.
 *
 * @param item the item providing the binding data.
 * @param itemClickedListener listener called when an event is clicked.
 */
fun ItemTriggerEventBinding.bind(item: UiTriggerEvent, itemClickedListener: (TriggerEvent) -> Unit) {
    textName.text = item.name
    textConditionsCount.text = item.conditionsCountText

    val actionColor = root.context.getThemeColor(if (item.haveError) R.attr.colorError else R.attr.colorOnSurface)
    textActionsCount.text = item.actionsCountText
    textActionsCount.setTextColor(actionColor)
    imageAction.setIconTintColor(actionColor)

    textEnabled.setText(item.enabledOnStartTextRes)
    iconEnabled.setImageResource(item.enabledOnStartIconRes)

    root.setOnClickListener { itemClickedListener(item.event) }
}