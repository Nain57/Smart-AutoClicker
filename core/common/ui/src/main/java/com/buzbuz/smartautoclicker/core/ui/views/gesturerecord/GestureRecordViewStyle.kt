
package com.buzbuz.smartautoclicker.core.ui.views.gesturerecord

import android.content.res.TypedArray
import android.graphics.Color
import androidx.annotation.ColorInt

import com.buzbuz.smartautoclicker.core.ui.R


internal class GestureRecorderViewStyle(
    @ColorInt val color: Int,
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