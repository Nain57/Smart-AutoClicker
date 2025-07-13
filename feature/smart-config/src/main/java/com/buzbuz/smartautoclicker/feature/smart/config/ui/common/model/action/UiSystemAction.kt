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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.feature.smart.config.R

@DrawableRes
internal fun getSystemActionIconRes(): Int = R.drawable.ic_action_system

internal fun SystemAction.getDescription(context: Context, inError: Boolean): String {
    if (inError) return context.getString(R.string.item_error_action_invalid_generic)

    return context.getString(R.string.item_system_action_details_text, type.toDisplayString(context))
}

internal fun SystemAction.Type.toDisplayString(context: Context): String =
    when (this) {
        SystemAction.Type.BACK -> context.getString(R.string.field_dropdown_system_action_type_back)
        SystemAction.Type.HOME -> context.getString(R.string.field_dropdown_system_action_type_home)
        SystemAction.Type.RECENT_APPS -> context.getString(R.string.field_dropdown_system_action_type_recent_apps)
    }