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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R


@Suppress("UnusedReceiverParameter")
internal val Action.Click.iconRes: Int
    @DrawableRes get() = R.drawable.ic_click

internal fun Action.Click.getDescription(context: Context, inError: Boolean): String = when {
    inError -> context.getString(R.string.item_error_action_invalid_generic)

    positionType == Action.Click.PositionType.ON_DETECTED_CONDITION ->
        context.getString(R.string.item_click_details_on_condition)

    else  -> context.getString(
        R.string.item_click_details_at_position,
        formatDuration(pressDuration!!), x, y,
    )
}