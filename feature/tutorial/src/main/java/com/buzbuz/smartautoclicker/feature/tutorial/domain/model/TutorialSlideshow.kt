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
package com.buzbuz.smartautoclicker.feature.tutorial.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes


data class TutorialSlideshow(
    val type: Type,
    @field:StringRes val nameRes: Int,
    @field:StringRes val shortDescriptionRes: Int,
    val slideshowItems: List<SlideshowItem>,
) {

    data class SlideshowItem(
        @field:StringRes val tutorialTextRes: Int,
        @field:DrawableRes val tutorialImage: Int,
        val tutorialImageFormat: ImageFormat,
    )

    enum class ImageFormat(val widthDp: Int, val heightDp: Int, val marginDp: Int = 0) {
        ICON(48, 48, 16),
        IMAGE_SQUARE(128, 128),
        IMAGE_LARGE(256, 128),
    }

    enum class Type {
        BROADCAST_RECEIVED_CONDITION,
        COUNTER_REACHED_CONDITION,
        IMAGE_CONDITION_CAPTURE,
        IMAGE_CONDITION_DETECTION_AREA,
        NUMBER_CONDITION_DETECTION_AREA,
        SCREEN_CONDITIONS_TYPE,
        SCREEN_CONDITIONS_DETECTION_THRESHOLD,
        TEXT_CONDITION_DETECTION_AREA
    }
}