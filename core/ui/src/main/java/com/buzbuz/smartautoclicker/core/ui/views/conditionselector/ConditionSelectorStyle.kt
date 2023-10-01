/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.ui.views.conditionselector

import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PointF

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.CaptureComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DEFAULT_ZOOM_MAXIMUM
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DEFAULT_ZOOM_MINIMUM
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.SelectorComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.DEFAULT_HINTS_ICON_MARGIN
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.DEFAULT_HINTS_ICON_SIZE
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.DEFAULT_HINTS_PINCH_ICON_MARGIN
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.HintsComponentStyle

internal fun TypedArray.getAnimationsStyle() =
    AnimationsStyle(
        selectorBackgroundAlpha = getColor(
            R.styleable.ConditionSelectorView_colorBackground,
            Color.TRANSPARENT
        ).shr(24),
        hintFadeDuration = getInteger(
            R.styleable.ConditionSelectorView_hintsFadeDuration,
            DEFAULT_FADE_DURATION
        ).toLong(),
        hintAllFadeDelay = getInteger(
            R.styleable.ConditionSelectorView_hintsAllFadeDelay,
            DEFAULT_FADE_ALL_HINTS_DURATION
        ).toLong(),
        showSelectorAnimationDuration = getInteger(
            R.styleable.ConditionSelectorView_showSelectorAnimationDuration,
            DEFAULT_SELECTOR_ANIMATION_DURATION
        ).toLong(),
        showCaptureAnimationDuration = getInteger(
            R.styleable.ConditionSelectorView_showCaptureAnimationDuration,
            DEFAULT_CAPTURE_ANIMATION_DURATION
        ).toLong(),
    )

internal fun TypedArray.getCaptureComponentStyle(displayMetrics: DisplayMetrics) =
    CaptureComponentStyle(
        displayMetrics = displayMetrics,
        zoomMin = getFloat(
            R.styleable.ConditionSelectorView_minimumZoomValue,
            DEFAULT_ZOOM_MINIMUM
        ),
        zoomMax = getFloat(
            R.styleable.ConditionSelectorView_maximumZoomValue,
            DEFAULT_ZOOM_MAXIMUM
        )
    )

internal fun TypedArray.getSelectorComponentStyle(displayMetrics: DisplayMetrics) =
    SelectorComponentStyle(
        displayMetrics = displayMetrics,
        selectorDefaultSize = PointF(
            getDimensionPixelSize(
                R.styleable.ConditionSelectorView_defaultWidth,
                100,
            ).toFloat() / 2f,
            getDimensionPixelSize(
                R.styleable.ConditionSelectorView_defaultHeight,
                100,
            ).toFloat() / 2f
        ),
        handleSize = getDimensionPixelSize(
            R.styleable.ConditionSelectorView_resizeHandleSize,
            10,
        ).toFloat(),
        selectorAreaOffset = kotlin.math.ceil(
            getDimensionPixelSize(
                R.styleable.ConditionSelectorView_thickness,
                4,
            ).toFloat() / 2
        ).toInt(),
        cornerRadius = getDimensionPixelSize(
            R.styleable.ConditionSelectorView_cornerRadius,
            2,
        ).toFloat(),
        selectorThickness = getDimensionPixelSize(
            R.styleable.ConditionSelectorView_thickness,
            4,
        ).toFloat(),
        selectorColor = getColor(
            R.styleable.ConditionSelectorView_colorOutlinePrimary,
            Color.WHITE,
        ),
        selectorBackgroundColor = getColor(
            R.styleable.ConditionSelectorView_colorBackground,
            Color.TRANSPARENT,
        ),
    )

internal fun TypedArray.getHintsStyle(displayMetrics: DisplayMetrics) =
    HintsComponentStyle(
        displayMetrics = displayMetrics,
        iconsMargin = getDimensionPixelSize(
            R.styleable.ConditionSelectorView_hintsIconsMargin,
            DEFAULT_HINTS_ICON_MARGIN,
        ),
        iconsSize = getDimensionPixelSize(
            R.styleable.ConditionSelectorView_hintsIconsSize,
            DEFAULT_HINTS_ICON_SIZE,
        ),
        moveIcon = getResourceId(R.styleable.ConditionSelectorView_hintMoveIcon, 0),
        upIcon = getResourceId(R.styleable.ConditionSelectorView_hintResizeUpIcon, 0),
        downIcon = getResourceId(R.styleable.ConditionSelectorView_hintResizeDownIcon, 0),
        leftIcon = getResourceId(R.styleable.ConditionSelectorView_hintResizeLeftIcon, 0),
        rightIcon = getResourceId(R.styleable.ConditionSelectorView_hintResizeRightIcon, 0),
        pinchIcon = getResourceId(R.styleable.ConditionSelectorView_hintPinchIcon, 0),
        pinchIconMargin = getDimensionPixelSize(
            R.styleable.ConditionSelectorView_hintsPinchIconMargin,
            DEFAULT_HINTS_PINCH_ICON_MARGIN,
        ),
    )
