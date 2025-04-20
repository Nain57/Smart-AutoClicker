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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TextCondition
import com.buzbuz.smartautoclicker.feature.smart.config.R


sealed class UiScreenCondition : UiCondition() {
    abstract override val condition: ScreenCondition

    abstract val shouldBeVisibleIconRes: Int
    abstract val shouldBeVisibleTextRes: Int
    abstract val detectionTypeIconRes: Int
    abstract val thresholdText: String
}

fun ScreenCondition.toUiScreenCondition(context: Context, inError: Boolean) =
    when (this) {
        is ImageCondition -> toUiImageCondition(context, inError)
        is TextCondition -> toUiTextCondition(context, inError)
    }


data class UiImageCondition(
    override val condition: ImageCondition,
    override val name: String,
    override val haveError: Boolean,
    @DrawableRes override val shouldBeVisibleIconRes: Int,
    @StringRes override val shouldBeVisibleTextRes: Int,
    @DrawableRes override val detectionTypeIconRes: Int,
    override val thresholdText: String,
) : UiScreenCondition()

fun ImageCondition.toUiImageCondition(context: Context, inError: Boolean) = UiImageCondition(
    condition = this,
    name = name,
    shouldBeVisibleIconRes = getShouldBeDetectedIconRes(),
    shouldBeVisibleTextRes = getShouldBeDetectedTextRes(),
    detectionTypeIconRes = getDetectionTypeIconRes(),
    thresholdText = getThresholdText(context),
    haveError = inError,
)


data class UiTextCondition(
    override val condition: TextCondition,
    override val name: String,
    override val haveError: Boolean,
    @DrawableRes override val shouldBeVisibleIconRes: Int,
    @StringRes override val shouldBeVisibleTextRes: Int,
    @DrawableRes override val detectionTypeIconRes: Int,
    override val thresholdText: String,
    val conditionTextDescription : String,
) : UiScreenCondition()

fun TextCondition.toUiTextCondition(context: Context, inError: Boolean) = UiTextCondition(
    condition = this,
    name = name,
    shouldBeVisibleIconRes = getShouldBeDetectedIconRes(),
    shouldBeVisibleTextRes = getShouldBeDetectedTextRes(),
    detectionTypeIconRes = getDetectionTypeIconRes(),
    thresholdText = getThresholdText(context),
    conditionTextDescription = textToDetect,
    haveError = inError,
)


@DrawableRes
private fun ScreenCondition.getShouldBeDetectedIconRes(): Int =
    if (shouldBeDetected) R.drawable.ic_confirm else R.drawable.ic_cancel

@StringRes
private fun ScreenCondition.getShouldBeDetectedTextRes(): Int =
    if (shouldBeDetected) R.string.item_image_condition_visible else R.string.item_image_condition_not_visible

@DrawableRes
private fun ScreenCondition.getDetectionTypeIconRes(): Int =
    when (detectionType) {
        EXACT -> R.drawable.ic_detect_exact
        WHOLE_SCREEN -> R.drawable.ic_detect_whole_screen
        IN_AREA -> R.drawable.ic_detect_in_area
        else -> throw IllegalStateException("Can't get detection type icon, unknown type $detectionType")
    }

private fun ScreenCondition.getThresholdText(context: Context): String =
    context.getString(
        R.string.item_image_condition_desc_threshold,
        threshold,
    )
