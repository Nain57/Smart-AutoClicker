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
package com.buzbuz.smartautoclicker.core.ui.views.imageselector

import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PointF

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.pixelselector.PixelSelectorView
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.CaptureComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DEFAULT_ZOOM_MAXIMUM
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DEFAULT_ZOOM_MINIMUM
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.PixelPositionComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.SelectorComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.DEFAULT_HINTS_ICON_MARGIN
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.DEFAULT_HINTS_ICON_SIZE
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.DEFAULT_HINTS_PINCH_ICON_MARGIN
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.hints.HintsComponentStyle
import kotlin.math.ceil

internal fun TypedArray.getAnimationsStyle() =
    AnimationsStyle(
        selectorBackgroundAlpha = getColor(
            R.styleable.ImageSelectorView_colorBackground,
            Color.TRANSPARENT
        ).shr(24),
        hintFadeDuration = getInteger(
            R.styleable.ImageSelectorView_hintsFadeDuration,
            DEFAULT_FADE_DURATION
        ).toLong(),
        hintAllFadeDelay = getInteger(
            R.styleable.ImageSelectorView_hintsAllFadeDelay,
            DEFAULT_FADE_ALL_HINTS_DURATION
        ).toLong(),
        showSelectorAnimationDuration = getInteger(
            R.styleable.ImageSelectorView_showSelectorAnimationDuration,
            DEFAULT_SELECTOR_ANIMATION_DURATION
        ).toLong(),
        showCaptureAnimationDuration = getInteger(
            R.styleable.ImageSelectorView_showCaptureAnimationDuration,
            DEFAULT_CAPTURE_ANIMATION_DURATION
        ).toLong(),
    )

internal fun TypedArray.getCaptureComponentStyle(displayConfigManager: DisplayConfigManager) =
    CaptureComponentStyle(
        displayConfigManager = displayConfigManager,
        zoomMin = getFloat(
            R.styleable.ImageSelectorView_minimumZoomValue,
            DEFAULT_ZOOM_MINIMUM
        ),
        zoomMax = getFloat(
            R.styleable.ImageSelectorView_maximumZoomValue,
            DEFAULT_ZOOM_MAXIMUM
        )
    )

internal fun TypedArray.getSelectorComponentStyle(displayConfigManager: DisplayConfigManager) =
    SelectorComponentStyle(
        displayConfigManager = displayConfigManager,
        selectorDefaultSize = PointF(
            getDimensionPixelSize(
                R.styleable.ImageSelectorView_defaultWidth,
                100,
            ).toFloat() / 2f,
            getDimensionPixelSize(
                R.styleable.ImageSelectorView_defaultHeight,
                100,
            ).toFloat() / 2f
        ),
        handleSize = getDimensionPixelSize(
            R.styleable.ImageSelectorView_resizeHandleSize,
            10,
        ).toFloat(),
        selectorAreaOffset = ceil(
            getDimensionPixelSize(
                R.styleable.ImageSelectorView_thickness,
                4,
            ).toFloat() / 2
        ).toInt(),
        cornerRadius = getDimensionPixelSize(
            R.styleable.ImageSelectorView_cornerRadius,
            2,
        ).toFloat(),
        selectorThickness = getDimensionPixelSize(
            R.styleable.ImageSelectorView_thickness,
            4,
        ).toFloat(),
        selectorColor = getColor(
            R.styleable.ImageSelectorView_colorOutlinePrimary,
            Color.WHITE,
        ),
        selectorBackgroundColor = getColor(
            R.styleable.ImageSelectorView_colorBackground,
            Color.TRANSPARENT,
        ),
    )

internal fun TypedArray.getHintsStyle(displayConfigManager: DisplayConfigManager) =
    HintsComponentStyle(
        displayConfigManager = displayConfigManager,
        iconsMargin = getDimensionPixelSize(
            R.styleable.ImageSelectorView_hintsIconsMargin,
            DEFAULT_HINTS_ICON_MARGIN,
        ),
        iconsSize = getDimensionPixelSize(
            R.styleable.ImageSelectorView_hintsIconsSize,
            DEFAULT_HINTS_ICON_SIZE,
        ),
        moveIcon = getResourceId(R.styleable.ImageSelectorView_hintMoveIcon, 0),
        upIcon = getResourceId(R.styleable.ImageSelectorView_hintResizeUpIcon, 0),
        downIcon = getResourceId(R.styleable.ImageSelectorView_hintResizeDownIcon, 0),
        leftIcon = getResourceId(R.styleable.ImageSelectorView_hintResizeLeftIcon, 0),
        rightIcon = getResourceId(R.styleable.ImageSelectorView_hintResizeRightIcon, 0),
        pinchIcon = getResourceId(R.styleable.ImageSelectorView_hintPinchIcon, 0),
        pinchIconMargin = getDimensionPixelSize(
            R.styleable.ImageSelectorView_hintsPinchIconMargin,
            DEFAULT_HINTS_PINCH_ICON_MARGIN,
        ),
    )
