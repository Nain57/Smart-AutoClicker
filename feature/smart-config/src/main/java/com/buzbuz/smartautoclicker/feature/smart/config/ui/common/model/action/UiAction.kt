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
import com.buzbuz.smartautoclicker.core.domain.model.event.Event


data class UiAction internal constructor(
    @DrawableRes val icon: Int,
    val name: String,
    val description: String,
    val action: Action,
    val haveError: Boolean,
)

internal fun Action.toUiAction(context: Context, parent: Event, inError: Boolean = !isComplete()): UiAction =
    UiAction(
        action = this,
        name = name!!,
        icon = getIconRes(),
        description = getActionDescription(context, parent, inError),
        haveError = inError,
    )

@DrawableRes
internal fun Action.getIconRes(): Int = when (this) {
    is Action.Click -> getClickIconRes()
    is Action.Swipe -> getSwipeIconRes()
    is Action.Pause -> getPauseIconRes()
    is Action.Intent -> getIntentIconRes()
    is Action.ToggleEvent -> getToggleEventIconRes()
    is Action.ChangeCounter -> getChangeCounterIconRes()
    else -> throw IllegalArgumentException("Not yet supported")
}

internal fun Action.getActionDescription(context: Context, parent: Event, inError: Boolean): String = when (this) {
    is Action.Click -> getDescription(context, parent, inError)
    is Action.Swipe -> getDescription(context, inError)
    is Action.Pause -> getDescription(context, inError)
    is Action.Intent -> getDescription(context, inError)
    is Action.ToggleEvent -> getDescription(context, inError)
    is Action.ChangeCounter -> getDescription(context, inError)
    else -> throw IllegalArgumentException("Not yet supported")
}
