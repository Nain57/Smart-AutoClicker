
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.selection

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.DialogChoice
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getChangeCounterIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getClickIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getHideKeyboardIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getIntentIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getLongPressIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getNotificationIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getPauseIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getScreenshotIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getScrollIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getShowKeyboardIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getSwipeIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getToggleEventIconRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.getTypeTextIconRes


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
        getClickIconRes(),
    )
    /** Swipe Action choice. */
    data object Swipe : ActionTypeChoice(
        R.string.item_swipe_title,
        R.string.item_swipe_desc,
        getSwipeIconRes(),
    )
    /** Pause Action choice. */
    data object Pause : ActionTypeChoice(
        R.string.item_pause_title,
        R.string.item_pause_desc,
        getPauseIconRes(),
    )
    /** Intent Action choice. */
    data object Intent : ActionTypeChoice(
        R.string.item_intent_title,
        R.string.item_intent_desc,
        getIntentIconRes(),
    )
    /** Toggle Event Action choice. */
    data object ToggleEvent : ActionTypeChoice(
        R.string.item_toggle_event_title,
        R.string.item_toggle_event_desc,
        getToggleEventIconRes(),
    )

    /** Change counter Action choice. */
    data object ChangeCounter : ActionTypeChoice(
        R.string.item_change_counter_title,
        R.string.item_change_counter_desc,
        getChangeCounterIconRes(),
    )

    /** Notification Action choice. */
    data object Notification : ActionTypeChoice(
        R.string.item_notification_title,
        R.string.item_notification_desc,
        getNotificationIconRes(),
    )

    /** Long press Action choice. */
    data object LongPress : ActionTypeChoice(
        R.string.item_long_press_title,
        R.string.item_long_press_desc,
        getLongPressIconRes(),
    )

    /** Scroll Action choice. */
    data object Scroll : ActionTypeChoice(
        R.string.item_scroll_title,
        R.string.item_scroll_desc,
        getScrollIconRes(),
    )

    /** Screenshot Action choice. */
    data object Screenshot : ActionTypeChoice(
        R.string.item_screenshot_title,
        R.string.item_screenshot_desc,
        getScreenshotIconRes(),
    )

    /** Hide keyboard Action choice. */
    data object HideKeyboard : ActionTypeChoice(
        R.string.item_hide_keyboard_title,
        R.string.item_hide_keyboard_desc,
        getHideKeyboardIconRes(),
    )

    /** Show keyboard Action choice. */
    data object ShowKeyboard : ActionTypeChoice(
        R.string.item_show_keyboard_title,
        R.string.item_show_keyboard_desc,
        getShowKeyboardIconRes(),
    )

    /** Type text Action choice. */
    data object TypeText : ActionTypeChoice(
        R.string.item_type_text_title,
        R.string.item_type_text_desc,
        getTypeTextIconRes(),
    )
}