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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.trigger

import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.DialogChoice
import com.buzbuz.smartautoclicker.feature.smart.config.R


/** Choices for the dumb action type selection dialog. */
sealed class TriggerConditionTypeChoice(
    title: Int,
    description: Int,
    iconId: Int?,
): DialogChoice(
    title = title,
    description = description,
    iconId = iconId,
) {
    data object OnBroadcastReceived : TriggerConditionTypeChoice(
        R.string.item_title_broadcast_received,
        R.string.item_desc_broadcast_received,
        R.drawable.ic_broadcast_received,
    )
    data object OnCounterReached : TriggerConditionTypeChoice(
        R.string.item_title_counter_reached,
        R.string.item_desc_counter_reached,
        R.drawable.ic_counter_reached,
    )
    data object OnTimerReached : TriggerConditionTypeChoice(
        R.string.item_title_timer_reached,
        R.string.item_desc_timer_reached,
        R.drawable.ic_timer_reached,
    )
}

fun allTriggerConditionChoices() = listOf(
    TriggerConditionTypeChoice.OnBroadcastReceived,
    TriggerConditionTypeChoice.OnCounterReached,
    TriggerConditionTypeChoice.OnTimerReached,
)