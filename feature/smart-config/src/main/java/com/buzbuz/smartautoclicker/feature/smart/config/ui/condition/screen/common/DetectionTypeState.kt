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

import android.content.Context
import android.graphics.Rect
import com.buzbuz.smartautoclicker.core.domain.model.DetectionType
import com.buzbuz.smartautoclicker.feature.smart.config.R


data class DetectionTypeState(
    @DetectionType val type: Int,
    val areaText: String,
)

internal fun Context.getDetectionTypeState(@DetectionType type: Int, area: Rect) = DetectionTypeState(
    type = type,
    areaText = getString(R.string.field_select_detection_area_desc, area.left, area.top, area.right, area.bottom)
)