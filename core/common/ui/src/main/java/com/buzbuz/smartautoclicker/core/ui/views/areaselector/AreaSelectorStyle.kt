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
package com.buzbuz.smartautoclicker.core.ui.views.areaselector

import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PointF

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.SelectorComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.DEFAULT_HINTS_ICON_MARGIN
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.DEFAULT_HINTS_ICON_SIZE
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.HintsComponentStyle

import kotlin.math.ceil

internal fun TypedArray.getAnimationsStyle() =
    AnimationsStyle(
        selectorBackgroundAlpha = getColor(
            R.styleable.AreaSelectorView_colorBackground,
            Color.TRANSPARENT
        ).shr(24),
        hintFadeDuration = getInteger(
            R.styleable.AreaSelectorView_hintsFadeDuration,
            DEFAULT_FADE_DURATION
        ).toLong(),
        hintAllFadeDelay = getInteger(
            R.styleable.AreaSelectorView_hintsAllFadeDelay,
            DEFAULT_FADE_ALL_HINTS_DURATION
        ).toLong(),
        showSelectorAnimationDuration = getInteger(
            R.styleable.AreaSelectorView_showSelectorAnimationDuration,
            DEFAULT_SELECTOR_ANIMATION_DURATION
        ).toLong(),
    )

internal fun TypedArray.getSelectorComponentStyle(displayMetrics: DisplayMetrics) =
    SelectorComponentStyle(
        displayMetrics = displayMetrics,
        selectorDefaultSize = PointF(
            getDimensionPixelSize(
                R.styleable.AreaSelectorView_defaultWidth,
                100,
            ).toFloat() / 2f,
            getDimensionPixelSize(
                R.styleable.AreaSelectorView_defaultHeight,
                100,
            ).toFloat() / 2f
        ),
        handleSize = getDimensionPixelSize(
            R.styleable.AreaSelectorView_resizeHandleSize,
            10,
        ).toFloat(),
        selectorAreaOffset = ceil(
            getDimensionPixelSize(
                R.styleable.AreaSelectorView_thickness,
                4,
            ).toFloat() / 2
        ).toInt(),
        cornerRadius = getDimensionPixelSize(
            R.styleable.AreaSelectorView_cornerRadius,
            2,
        ).toFloat(),
        selectorThickness = getDimensionPixelSize(
            R.styleable.AreaSelectorView_thickness,
            4,
        ).toFloat(),
        selectorColor = getColor(
            R.styleable.AreaSelectorView_colorOutlinePrimary,
            Color.WHITE,
        ),
        selectorBackgroundColor = getColor(
            R.styleable.AreaSelectorView_colorBackground,
            Color.TRANSPARENT,
        ),
    )

internal fun TypedArray.getHintsStyle(displayMetrics: DisplayMetrics) =
    HintsComponentStyle(
        displayMetrics = displayMetrics,
        iconsMargin = getDimensionPixelSize(
            R.styleable.AreaSelectorView_hintsIconsMargin,
            DEFAULT_HINTS_ICON_MARGIN,
        ),
        iconsSize = getDimensionPixelSize(
            R.styleable.AreaSelectorView_hintsIconsSize,
            DEFAULT_HINTS_ICON_SIZE,
        ),
        moveIcon = getResourceId(R.styleable.AreaSelectorView_hintMoveIcon, 0),
        upIcon = getResourceId(R.styleable.AreaSelectorView_hintResizeUpIcon, 0),
        downIcon = getResourceId(R.styleable.AreaSelectorView_hintResizeDownIcon, 0),
        leftIcon = getResourceId(R.styleable.AreaSelectorView_hintResizeLeftIcon, 0),
        rightIcon = getResourceId(R.styleable.AreaSelectorView_hintResizeRightIcon, 0),
    )