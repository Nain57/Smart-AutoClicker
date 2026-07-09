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

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo

interface TutorialItem {

    fun getType(): Type
    fun getTutorialInfo(): TutorialInfo
    fun getTutorial(): Tutorial

    enum class Type {
        COMBINE_CONDITIONS_NOT_VISIBLE,
        COLOR_CONDITION,
        IMAGE_DETECTION_MOVING_TARGET,
        IMAGE_DETECTION_STILL_TARGET,
        NUMBER_CONDITION_STATIC_VALUE,
        TEXT_CONDITION_MOVING_TEXT,
        TEXT_CONDITION_STILL_TEXT,
        TIMER_REACHED_CONDITION;

        fun toTutorialId(): String = name
    }

}