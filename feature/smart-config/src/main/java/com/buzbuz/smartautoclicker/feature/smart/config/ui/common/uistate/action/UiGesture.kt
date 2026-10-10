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
import com.buzbuz.smartautoclicker.core.domain.model.action.Gesture
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.ui.utils.formatDuration
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.GestureDialogUiStateItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.action.gesture.ReleaseWhenDropdownItem
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.UiGesturePosition


@DrawableRes
internal fun getGestureIconRes(): Int =
    R.drawable.ic_gesture

internal fun Gesture.getDescription(context: Context, inError: Boolean): String = when {
    inError -> context.getString(R.string.item_error_action_invalid_generic)

    else -> context.getString(
        R.string.item_swipe_details,
        formatDuration(getDurationMs()),
    )
}

internal fun Context.getGestureUiStateItemDescription(motion: GestureDialogUiStateItem): String =
    when (motion) {
        is GestureDialogUiStateItem.Move -> getMoveDescription(motion.position)
        is GestureDialogUiStateItem.Press -> getPressDescription(motion.position)
        is GestureDialogUiStateItem.Release -> getReleaseDescription(motion)
        is GestureDialogUiStateItem.Wait -> getString(R.string.item_desc_gesture_wait, motion.durationMs)

        is GestureDialogUiStateItem.Add,
        is GestureDialogUiStateItem.Header -> throw IllegalArgumentException("Unsupported motion item")
    }

internal fun Context.getReleaseDescription(release: GestureDialogUiStateItem.Release): String =
    when (release.whenItem) {
        ReleaseWhenDropdownItem.Now -> getString(R.string.item_desc_gesture_release_now)
        ReleaseWhenDropdownItem.UntilActionsEnd -> getString(R.string.item_desc_gesture_release_until_actions_end)
        ReleaseWhenDropdownItem.OnceUnfulfilled -> getString(R.string.item_desc_gesture_release_once_unfulfilled)
        ReleaseWhenDropdownItem.ForDuration ->
            getString(R.string.item_desc_gesture_release_for_duration, release.holdDurationMs ?: 0L)
    }

internal fun Context.getMoveDescription(position: UiGesturePosition): String =
    when (position) {
        is UiGesturePosition.Condition ->
            if (position.inError) getString(R.string.item_desc_gesture_move_condition_error)
            else if (position.condition == null) getString(R.string.item_desc_gesture_move_condition_or)
            else getString(R.string.item_desc_gesture_move_condition, position.condition.name)

        is UiGesturePosition.Position -> {
            getString(
                R.string.item_desc_gesture_move_value,
                position.xValue,
                position.yValue,
            )
        }
    }

internal fun Context.getPressDescription(position: UiGesturePosition): String =
    when (position) {
        is UiGesturePosition.Condition ->
            if (position.inError) getString(R.string.item_desc_gesture_press_condition_error)
            else if (position.condition == null) getString(R.string.item_desc_gesture_press_condition_or)
            else getString(R.string.item_desc_gesture_press_condition, position.condition.name)

        is UiGesturePosition.Position -> {
            getString(
                R.string.item_desc_gesture_press_value,
                position.xValue,
                position.yValue,
            )
        }
    }

internal fun Context.getOffsetDescription(position: UiGesturePosition): String =
    if (position !is UiGesturePosition.Position) return ""
    else getString(R.string.field_click_offset_desc, position.xValue, position.yValue)

internal fun Context.getClickOnConditionTitle(condition: Condition?, inError: Boolean): String =
    if (inError) getString(R.string.message_title_gesture_on_condition_error)
    else if (condition != null) getString(R.string.message_title_gesture_on_condition_and)
    else getString(R.string.message_title_gesture_on_condition_or)

internal fun Context.getClickOnConditionDescription(condition: Condition?, inError: Boolean): String =
    if (inError) getString(R.string.message_desc_gesture_on_condition_error)
    else if (condition != null) getString(R.string.message_desc_gesture_on_condition_and, condition.name)
    else getString(R.string.message_desc_gesture_on_condition_or)