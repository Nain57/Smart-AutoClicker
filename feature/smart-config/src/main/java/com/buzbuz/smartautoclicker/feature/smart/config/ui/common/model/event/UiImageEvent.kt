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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.R


data class UiImageEvent(
    override val event: ScreenEvent,
    val name: String,
    val conditionsCountText: String,
    val actionsCountText: String,
    @StringRes val enabledOnStartTextRes: Int,
    @DrawableRes val enabledOnStartIconRes: Int,
    val haveError: Boolean,
) : UiEvent()

fun ScreenEvent.toUiImageEvent(inError: Boolean): UiImageEvent {
    @StringRes val enabledOnStartTextRes: Int
    @DrawableRes val enabledOnStartIconRes: Int
    if (enabledOnStart) {
        enabledOnStartTextRes = R.string.item_event_desc_enabled_children
        enabledOnStartIconRes = R.drawable.ic_confirm
    } else {
        enabledOnStartTextRes = R.string.item_event_desc_disabled_children
        enabledOnStartIconRes = R.drawable.ic_cancel
    }

    return UiImageEvent(
        event = this,
        name = name,
        conditionsCountText = conditions.size.toString(),
        actionsCountText = actions.size.toString(),
        enabledOnStartTextRes = enabledOnStartTextRes,
        enabledOnStartIconRes = enabledOnStartIconRes,
        haveError = inError,
    )
}