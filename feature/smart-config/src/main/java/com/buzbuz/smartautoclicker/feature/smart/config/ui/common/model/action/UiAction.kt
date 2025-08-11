
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.HideKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.LongPress
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Screenshot
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.Scroll
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.ShowKeyboard
import com.buzbuz.smartautoclicker.core.domain.model.action.Action.TypeText
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.action.Pause
import com.buzbuz.smartautoclicker.core.domain.model.action.Swipe
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.Event


data class UiAction(
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
    is Click -> getClickIconRes()
    is Swipe -> getSwipeIconRes()
    is Pause -> getPauseIconRes()
    is Intent -> getIntentIconRes()
    is ToggleEvent -> getToggleEventIconRes()
    is ChangeCounter -> getChangeCounterIconRes()
    is Notification -> getNotificationIconRes()

    is LongPress -> getLongPressIconRes()
    is Scroll -> getScrollIconRes()
    is Screenshot -> getScreenshotIconRes()
    is HideKeyboard -> getHideKeyboardIconRes()
    is ShowKeyboard -> getShowKeyboardIconRes()
    is TypeText -> getTypeTextIconRes()
    
    else -> throw IllegalArgumentException("Not yet supported")
}

internal fun Action.getActionDescription(context: Context, parent: Event, inError: Boolean): String = when (this) {
    is Click -> getDescription(context, parent, inError)
    is Swipe -> getDescription(context, inError)
    is Pause -> getDescription(context, inError)
    is Intent -> getDescription(context, inError)
    is ToggleEvent -> getDescription(context, inError)
    is ChangeCounter -> getDescription(context, inError)
    is Notification -> getDescription(context, inError)

    is LongPress -> getDescription(context, parent, inError)
    is Scroll -> getDescription(context, parent, inError)
    is Screenshot -> getDescription(context, parent, inError)
    is HideKeyboard -> getDescription(context, parent, inError)
    is ShowKeyboard -> getDescription(context, parent, inError)
    is TypeText -> getDescription(context, parent, inError)


    else -> throw IllegalArgumentException("Not yet supported")
}
