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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items

data class TutorialCategory(
    val type: Type,
    val nameRes: Int,
    val shortDescriptionRes: Int,
    val longDescriptionRes: Int,
    val iconRes: Int,
    val content: List<Content>,
) {

    sealed interface Content {
        data class Category(val type: Type) : Content
        data class Tutorial(val type: TutorialItem.Type) : Content
    }

    enum class Type {
        ACTIONS,
        BASICS,
        COLOR_CONDITION,
        IMAGE_CONDITION,
        NUMBER_CONDITION,
        ROOT,
        SCREEN_CONDITIONS,
        TEXT_CONDITION,
        TRIGGER_CONDITIONS,
    }
}
