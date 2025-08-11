
package com.buzbuz.smartautoclicker.core.ui.views.areaselector

import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PointF

import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
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

internal fun TypedArray.getSelectorComponentStyle(displayConfigManager: DisplayConfigManager) =
    SelectorComponentStyle(
        displayConfigManager = displayConfigManager,
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

internal fun TypedArray.getHintsStyle(displayConfigManager: DisplayConfigManager) =
    HintsComponentStyle(
        displayConfigManager = displayConfigManager,
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