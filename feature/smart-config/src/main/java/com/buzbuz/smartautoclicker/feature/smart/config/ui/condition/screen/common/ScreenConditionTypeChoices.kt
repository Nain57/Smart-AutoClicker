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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.common

import com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation.DialogChoice
import com.buzbuz.smartautoclicker.feature.smart.config.R


/** Choices for the screen condition type selection dialog. */
sealed class ScreenConditionTypeChoice(
    title: Int,
    description: Int,
    iconId: Int?,
): DialogChoice(
    title = title,
    description = description,
    iconId = iconId,
) {

    data object OnImageDetected : ScreenConditionTypeChoice(
        R.string.item_image_detected_title,
        R.string.item_image_detected_desc,
        R.drawable.ic_image_condition,
    )
    data object OnTextDetected : ScreenConditionTypeChoice(
        R.string.item_text_detected_title,
        R.string.item_text_detected_desc,
        R.drawable.ic_text_condition,
    )

}

fun allScreenConditionChoices() = listOf(
    ScreenConditionTypeChoice.OnImageDetected,
    ScreenConditionTypeChoice.OnTextDetected,
)