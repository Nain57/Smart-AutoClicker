/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.scenario.config.dumb.ui.actions

import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.DialogChoice
import com.buzbuz.smartautoclicker.feature.scenario.config.dumb.R


/** Choices for the dumb action type selection dialog. */
sealed class DumbActionTypeChoice(
    title: Int,
    description: Int,
    iconId: Int?,
): DialogChoice(
    title = title,
    description = description,
    iconId = iconId,
) {
    /** Click Action choice. */
    data object Click : DumbActionTypeChoice(
        R.string.item_title_dumb_click,
        R.string.item_desc_dumb_click,
        R.drawable.ic_click,
    )
    /** Swipe Action choice. */
    data object Swipe : DumbActionTypeChoice(
        R.string.item_title_dumb_swipe,
        R.string.item_desc_dumb_swipe,
        R.drawable.ic_swipe,
    )
    /** Pause Action choice. */
    data object Pause : DumbActionTypeChoice(
        R.string.item_title_dumb_pause,
        R.string.item_desc_dumb_pause,
        R.drawable.ic_wait,
    )
}

fun allDumbActionChoices() = listOf(
    DumbActionTypeChoice.Click,
    DumbActionTypeChoice.Swipe,
    DumbActionTypeChoice.Pause,
)