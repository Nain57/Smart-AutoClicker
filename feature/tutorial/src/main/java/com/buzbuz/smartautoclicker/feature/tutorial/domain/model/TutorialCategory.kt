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

data class TutorialCategory(
    val type: Type,
    @field:StringRes val nameRes: Int,
    @field:StringRes val shortDescriptionRes: Int,
    @field:StringRes val longDescriptionRes: Int,
    @field:DrawableRes val iconRes: Int,
    val content: List<Content>,
) {

    sealed interface Content {
        data object Divider : Content
        data class Category(val type: Type) : Content
        data class Tutorial(val type: TutorialItem.Type) : Content
        data class Slideshow(val type: TutorialSlideshow.Type) : Content
    }

    enum class Type {
        ACTIONS,
        BASICS,
        CHANGE_COUNTER_ACTION,
        CLICK_ACTION,
        COLOR_CONDITION,
        COMBINE_CONDITIONS,
        COUNTERS,
        COMBINE_EVENTS,
        EVENTS_PRIORITY,
        EVENTS_STATE,
        IMAGE_CONDITION,
        INTENT_ACTION,
        NOTIFICATION_ACTION,
        NUMBER_CONDITION,
        PAUSE_ACTION,
        ROOT,
        SCREEN_CONDITIONS,
        SWIPE_ACTION,
        SYSTEM_ACTION,
        TEXT_CONDITION,
        TOGGLE_EVENT_ACTION,
        TRIGGER_CONDITIONS,
        WRITE_TEXT_ACTION,
    }
}
