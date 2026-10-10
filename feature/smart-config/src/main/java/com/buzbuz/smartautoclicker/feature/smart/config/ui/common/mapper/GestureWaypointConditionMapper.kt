/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.mapper

import android.content.Context
import androidx.core.graphics.drawable.toBitmap

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.ext.getConditionBitmap
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.counter.CounterOperationValue
import com.buzbuz.smartautoclicker.core.ui.utils.createColorIndicatorDrawable
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.GestureWaypointCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters.toNameRes

/**
 * Creates the display of each condition, to be shown on the gesture waypoints placed on it.
 *
 * - Image: the condition bitmap, at its capture size.
 * - Color: the color indicator circle.
 * - Text: a label with the text to detect.
 * - Number: a label with the comparison operation, such as "≥ 12".
 *
 * @return the display of each condition, by condition identifier. Conditions that can't be displayed, like an image
 * with a missing bitmap, are absent.
 */
internal suspend fun List<ScreenCondition>.toWaypointConditions(
    context: Context,
    bitmapRepository: BitmapRepository,
): Map<Identifier, GestureWaypointCondition> =
    buildMap {
        this@toWaypointConditions.forEach { condition ->
            condition.toWaypointCondition(context, bitmapRepository)?.let { put(condition.id, it) }
        }
    }

private suspend fun ScreenCondition.toWaypointCondition(
    context: Context,
    bitmapRepository: BitmapRepository,
): GestureWaypointCondition? =
    when (this) {
        is ScreenCondition.Image ->
            bitmapRepository.getConditionBitmap(this)?.let { GestureWaypointCondition(bitmap = it) }

        is ScreenCondition.Color ->
            context.createColorIndicatorDrawable(color)?.toBitmap()?.let { GestureWaypointCondition(bitmap = it) }

        is ScreenCondition.Text ->
            text.takeIf { it.isNotEmpty() }?.let { GestureWaypointCondition(label = it) }

        is ScreenCondition.Number -> GestureWaypointCondition(
            label = "${context.getString(comparisonOperation.toNameRes())} ${counterValue.toDisplayText()}",
        )
    }

private fun CounterOperationValue.toDisplayText(): String =
    when (this) {
        is CounterOperationValue.Number ->
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        is CounterOperationValue.Counter -> counterName.ifEmpty { "?" }
    }
