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
package com.buzbuz.smartautoclicker.core.ui.views.pixelselector

import android.content.res.TypedArray
import android.graphics.Color
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.ui.R
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.CaptureComponentStyle
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DEFAULT_ZOOM_MAXIMUM
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.DEFAULT_ZOOM_MINIMUM
import com.buzbuz.smartautoclicker.core.ui.views.viewcomponents.PixelPositionComponentStyle


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

internal fun TypedArray.getPixelPositionComponentStyle(displayConfigManager: DisplayConfigManager) =
    PixelPositionComponentStyle(
        displayConfigManager = displayConfigManager,
        color = getColor(
            R.styleable.PixelSelectorView_colorOutlinePrimary,
            Color.WHITE,
        ),
        thicknessPx = getDimensionPixelSize(
            R.styleable.PixelSelectorView_thickness,
            1,
        ).toFloat()
    )