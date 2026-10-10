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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.SetText
import com.buzbuz.smartautoclicker.core.domain.model.action.SystemAction
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState


data class UiAction(
    @param:DrawableRes val icon: Int,
    val name: String,
    val description: String,
    val action: Action,
    val haveError: Boolean,
)

internal fun Action.toUiAction(context: Context, parent: Event? = null, inError: Boolean = !isComplete()): UiAction =
    UiAction(
        action = this,
        name = name!!,
        icon = getIconRes(),
        description = getActionDescription(context, parent, inError),
        haveError = inError,
    )

internal fun EditedListState<Action>.toItemBriefs(context: Context, parent: Event): List<ItemBrief> =
    value?.mapIndexed { index, action ->
        ItemBrief(action.id, action.toUiAction(context, parent, inError = !itemValidity[index]))
    } ?: emptyList()

@DrawableRes
internal fun Action.getIconRes(): Int = when (this) {
    is Click -> getClickIconRes()
    is Gesture -> getGestureIconRes()
    is Pause -> getPauseIconRes()
    is Intent -> getIntentIconRes()
    is ToggleEvent -> getToggleEventIconRes()
    is ChangeCounter -> getChangeCounterIconRes()
    is Notification -> getNotificationIconRes()
    is SystemAction -> getSystemActionIconRes()
    is SetText -> getSetTextIconRes()
}

internal fun Action.getActionDescription(context: Context, parent: Event?, inError: Boolean): String = when (this) {
    is Click -> getDescription(context, parent, inError)
    is Gesture -> getDescription(context, inError)
    is Pause -> getDescription(context, inError)
    is Intent -> getDescription(context, inError)
    is ToggleEvent -> getDescription(context, inError)
    is ChangeCounter -> getDescription(context, parent, inError)
    is Notification -> getDescription(context, inError)
    is SystemAction -> getDescription(context, inError)
    is SetText -> getDescription(context, inError)
}
