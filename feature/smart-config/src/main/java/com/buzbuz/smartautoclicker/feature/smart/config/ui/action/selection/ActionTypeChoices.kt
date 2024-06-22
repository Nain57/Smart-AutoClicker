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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.DialogChoice
import com.buzbuz.smartautoclicker.feature.smart.config.R


/** Choices for the action type selection dialog. */
sealed class ActionTypeChoice(
    title: Int,
    description: Int,
    iconId: Int?,
): DialogChoice(
    title = title,
    description = description,
    iconId = iconId,
    disabledIconId = R.drawable.ic_pro_small,
) {
    /** Copy Action choice. */
    data object Copy : ActionTypeChoice(
        R.string.item_copy_title,
        R.string.item_copy_desc,
        R.drawable.ic_copy,
    )
    /** Click Action choice. */
    data object Click : ActionTypeChoice(
        R.string.item_click_title,
        R.string.item_click_desc,
        R.drawable.ic_click,
    )
    /** Swipe Action choice. */
    data object Swipe : ActionTypeChoice(
        R.string.item_swipe_title,
        R.string.item_swipe_desc,
        R.drawable.ic_swipe,
    )
    /** Pause Action choice. */
    data object Pause : ActionTypeChoice(
        R.string.item_pause_title,
        R.string.item_pause_desc,
        R.drawable.ic_wait,
    )
    /** Intent Action choice. */
    data object Intent : ActionTypeChoice(
        R.string.item_intent_title,
        R.string.item_intent_desc,
        R.drawable.ic_intent,
    )
    /** Toggle Event Action choice. */
    data object ToggleEvent : ActionTypeChoice(
        R.string.item_toggle_event_title,
        R.string.item_toggle_event_desc,
        R.drawable.ic_toggle_event,
    )

    /** Change counter Action choice. */
    data object ChangeCounter : ActionTypeChoice(
        R.string.item_change_counter_title,
        R.string.item_change_counter_desc,
        R.drawable.ic_change_counter,
    )
}