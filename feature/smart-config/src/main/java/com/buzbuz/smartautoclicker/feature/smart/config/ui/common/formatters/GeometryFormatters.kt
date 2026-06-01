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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.formatters

import android.content.Context
import android.graphics.Rect

import com.buzbuz.smartautoclicker.feature.smart.config.R


internal fun Rect.toAreaDisplayText(context: Context): String =
    if (isEmpty) context.getString(R.string.generic_detection_area_desc_empty)
    else context.getString(R.string.generic_area_desc, left, top, right, bottom)