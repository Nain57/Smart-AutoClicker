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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.ui.R

sealed class UiCondition {
    abstract val condition: Condition
    abstract val name: String
    abstract val haveError: Boolean
    abstract val iconRes: Int
}

internal fun Condition.toUiCondition(context: Context, shortThreshold: Boolean? = null, inError: Boolean): UiCondition =
    when (this) {
        is ScreenCondition -> toUiScreenCondition(context, shortThreshold = shortThreshold == true, inError = inError)
        is TriggerCondition -> toUiTriggerCondition(context, inError = inError)
    }

@DrawableRes
internal fun Condition.getIconRes(): Int =
    when (this) {
        is ScreenCondition.Color -> R.drawable.ic_color_condition
        is ScreenCondition.Image -> R.drawable.ic_image_condition
        is ScreenCondition.Number -> R.drawable.ic_number_condition
        is ScreenCondition.Text -> R.drawable.ic_text_condition
        is TriggerCondition.OnBroadcastReceived -> R.drawable.ic_broadcast_received
        is TriggerCondition.OnCounterCountReached -> R.drawable.ic_counter_reached
        is TriggerCondition.OnTimerReached -> R.drawable.ic_timer_reached
    }