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
package com.buzbuz.smartautoclicker.feature.tutorial.domain

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.mapping.toTutorialCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.mapping.toTutorialItem
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategoryUiItems
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategoryUiState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class GetTutorialCategoryUseCase @Inject constructor(
    private val tutorialRepository: TutorialRepository,
) {


    operator fun invoke(categoryType: TutorialCategory.Type = TutorialCategory.Type.ROOT): Flow<TutorialCategoryUiState> =
        tutorialRepository.tutorialsCompletionState
            .map { completionStateMap -> categoryType.toTutorialCategory().toUiState(completionStateMap) }
            .onStart { emit(TutorialCategoryUiState.Loading) }

    private fun TutorialCategory.toUiState(completionState: Map<String, Boolean>): TutorialCategoryUiState =
        TutorialCategoryUiState.Loaded(
            categoryNameRes = nameRes,
            items = buildList {
                add(this@toUiState.toHeaderUiItem())
                addAll(content.map { categoryContent -> categoryContent.toUiItem(completionState)})
            }
        )

    private fun TutorialCategory.toHeaderUiItem(): TutorialCategoryUiItems.Header =
        TutorialCategoryUiItems.Header(
            categoryNameRes = nameRes,
            descriptionRes = longDescriptionRes,
            iconRes = iconRes,
        )

    private fun TutorialCategory.Content.toUiItem(completionState: Map<String, Boolean>): TutorialCategoryUiItems.Item =
        when (this) {
            is TutorialCategory.Content.Category -> toCategoryUiItem()
            is TutorialCategory.Content.Tutorial -> toTutorialUiItem(completionState)
        }

    private fun TutorialCategory.Content.Category.toCategoryUiItem(): TutorialCategoryUiItems.Item.Category {
        val category = type.toTutorialCategory()
        return TutorialCategoryUiItems.Item.Category(
            type = type,
            nameRes = category.nameRes,
            descriptionRes = category.shortDescriptionRes,
            iconRes = category.iconRes,
        )
    }

    private fun TutorialCategory.Content.Tutorial.toTutorialUiItem(
        completionState: Map<String, Boolean>,
    ): TutorialCategoryUiItems.Item.Tutorial {
        val item = type.toTutorialItem()
        val info = item.getTutorialInfo()
        val isCompleted = completionState[info.id] == true

        return TutorialCategoryUiItems.Item.Tutorial(
            type = type,
            nameRes = info.nameResId,
            descriptionRes = info.descResId,
            tutorialCompleted = isCompleted,
        )
    }

}