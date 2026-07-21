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
        IMAGE_PORTRAIT(128, 256),
    }

    enum class Type {
        ACTIONS_LIST,
        BROADCAST_RECEIVED_CONDITION,
        CHANGE_COUNTER_ACTION,
        CLICK_ACTION_OFFSET,
        CLICK_ACTION_TARGET,
        COMBINE_CONDITIONS_ORDERING,
        COUNTER_REACHED_CONDITION,
        COUNTERS_VALUE_USAGES,
        EVENTS_STATE_BASICS,
        EVENTS_PRIORITY_BASICS,
        EVENTS_RELOADING,
        IMAGE_CONDITION_CAPTURE,
        IMAGE_CONDITION_DETECTION_AREA,
        INTENT_ACTION,
        NOTIFICATION_ACTION,
        NUMBER_CONDITION_DETECTION_AREA,
        PAUSE_ACTION,
        SCREEN_CONDITIONS_TYPE,
        SCREEN_CONDITIONS_DETECTION_THRESHOLD,
        SWIPE_ACTION,
        SYSTEM_ACTION,
        TOGGLE_EVENT_ACTION,
        TEXT_CONDITION_DETECTION_AREA,
        WRITE_TEXT_ACTION,
    }
}