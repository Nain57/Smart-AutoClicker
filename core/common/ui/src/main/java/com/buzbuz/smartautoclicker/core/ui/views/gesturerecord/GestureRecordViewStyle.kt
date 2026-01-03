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
package com.buzbuz.smartautoclicker.core.ui.views.gesturerecord

import android.content.res.TypedArray
import android.graphics.Color
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.ui.R


internal class GestureRecorderViewStyle(
    @field:ColorInt val color: Int,
    val thicknessPx: Int,
    val lengthPx: Int,
)

internal fun TypedArray.getGestureRecorderStyle() =
    GestureRecorderViewStyle(
        color = getColor(
            R.styleable.GestureRecordView_recorderColor,
            DEFAULT_GESTURE_RECORDER_COLOR,
        ),
        thicknessPx = getDimensionPixelSize(
            R.styleable.GestureRecordView_thickness,
            DEFAULT_GESTURE_RECORDER_THICKNESS_PX,
        ),
        lengthPx = getDimensionPixelSize(
            R.styleable.GestureRecordView_length,
            DEFAULT_GESTURE_RECORDER_LENGTH_DP,
        ),
    )

private const val DEFAULT_GESTURE_RECORDER_COLOR = Color.RED
private const val DEFAULT_GESTURE_RECORDER_THICKNESS_PX = 30
private const val DEFAULT_GESTURE_RECORDER_LENGTH_DP = 100