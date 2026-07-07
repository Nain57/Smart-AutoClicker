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


sealed interface TutorialCategoryUiState {

    data object Loading : TutorialCategoryUiState

    data class Loaded(
        @field:StringRes val categoryNameRes: Int,
        val items: List<TutorialCategoryUiItems>,
    ) : TutorialCategoryUiState
}

sealed interface TutorialCategoryUiItems {

    data class Header(
        @field:StringRes val categoryNameRes: Int,
        @field:StringRes val descriptionRes: Int,
        @field:DrawableRes val iconRes: Int,
    ): TutorialCategoryUiItems

    data object SectionDivider : TutorialCategoryUiItems

    sealed interface Item : TutorialCategoryUiItems {

        val nameRes: Int
        val descriptionRes: Int

        data class Category(
            val type: TutorialCategory.Type,
            @field:StringRes override val nameRes: Int,
            @field:StringRes override val descriptionRes: Int,
            @field:DrawableRes val iconRes: Int,
        ) : Item

        data class Tutorial(
            val type: TutorialItem.Type,
            @field:StringRes override val nameRes: Int,
            @field:StringRes override val descriptionRes: Int,
            val tutorialCompleted: Boolean,
        ) : Item

        data class Slideshow(
            val type: TutorialSlideshow.Type,
            @field:StringRes override val nameRes: Int,
            @field:StringRes override val descriptionRes: Int,
        ) : Item
    }
}
