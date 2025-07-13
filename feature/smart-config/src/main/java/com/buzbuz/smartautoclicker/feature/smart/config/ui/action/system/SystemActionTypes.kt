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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.system

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.R


sealed class SystemActionTypeItem(
    @StringRes title: Int,
) : DropdownItem(title) {

    data object Back : SystemActionTypeItem(
        title = R.string.field_dropdown_system_action_type_back,
    )

    data object Home : SystemActionTypeItem(
        title = R.string.field_dropdown_system_action_type_home,
    )

    data object RecentApps : SystemActionTypeItem(
        title = R.string.field_dropdown_system_action_type_recent_apps,
    )
}

internal val systemActionTypeItems: List<SystemActionTypeItem>
    get() = listOf(
        SystemActionTypeItem.Back,
        SystemActionTypeItem.Home,
        SystemActionTypeItem.RecentApps,
    )

internal fun SystemAction.Type.toTypeItem(): SystemActionTypeItem =
    when (this) {
        SystemAction.Type.BACK -> SystemActionTypeItem.Back
        SystemAction.Type.HOME -> SystemActionTypeItem.Home
        SystemAction.Type.RECENT_APPS -> SystemActionTypeItem.RecentApps
    }

internal fun SystemActionTypeItem.toSystemActionType(): SystemAction.Type =
    when (this) {
        SystemActionTypeItem.Back -> SystemAction.Type.BACK
        SystemActionTypeItem.Home -> SystemAction.Type.HOME
        SystemActionTypeItem.RecentApps -> SystemAction.Type.RECENT_APPS
    }