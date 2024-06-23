/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image.brief

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.graphics.Rect
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief.ItemBrief
import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.EXACT
import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.core.domain.model.WHOLE_SCREEN
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.renderers.ImageConditionDescription
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.model.EditedListState

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

import javax.inject.Inject


class ImageConditionsBriefViewModel @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val displayMetrics: DisplayMetrics,
    private val repository: IRepository,
    private val editionRepository: EditionRepository,
) : ViewModel() {

    private val editedConditions: Flow<EditedListState<ImageCondition>> =
        editionRepository.editionState.editedEventImageConditionsState

    private val currentFocusItemIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    private val focusedCondition: Flow<Pair<ImageCondition, Bitmap?>?> =
        combine(currentFocusItemIndex, editedConditions) { focusedIndex, conditions ->
            val conditionList = conditions.value ?: return@combine null
            if (focusedIndex !in conditionList.indices) return@combine null

            val condition = conditionList[focusedIndex]
            condition to repository.getConditionBitmap(condition)
        }.flowOn(ioDispatcher)

    val conditionVisualization: Flow<ImageConditionDescription?> = focusedCondition.map { focusedCondition ->
        focusedCondition?.first?.toItemDescription(displayMetrics.screenSize, focusedCondition.second)
    }

    val conditionBriefList: Flow<List<ItemBrief>> = editedConditions.map { conditions ->
        val conditionList = conditions.value ?: emptyList()
        conditionList.mapIndexed { index, condition -> condition.toItemBrief(context, !conditions.itemValidity[index]) }
    }


    fun setFocusedItemIndex(index: Int) {
        currentFocusItemIndex.value = index
    }

    fun createNewImageConditionFromCopy(condition: ImageCondition): ImageCondition =
        editionRepository.editedItemsBuilder.createNewImageConditionFrom(condition)

    fun deleteImageCondition(index: Int, force: Boolean = false): Boolean {
        val conditions = editionRepository.editionState.getEditedEventConditions<ImageCondition>()?.toMutableList() ?: return false
        if (index !in conditions.indices) return false

        editionRepository.startConditionEdition(conditions[index])
        if (!force && editionRepository.editionState.isEditedConditionReferencedByClick()) {
            editionRepository.stopConditionEdition()
            return false
        }

        editionRepository.deleteEditedAction()
        return true
    }

    fun getEditedScenario(): Scenario? = editionRepository.editionState.getScenario()
    fun startConditionEdition(condition: Condition) = editionRepository.startConditionEdition(condition)
    fun upsertEditedCondition() = editionRepository.upsertEditedCondition()
    fun removeEditedCondition() = editionRepository.deleteEditedCondition()
    fun dismissEditedCondition() = editionRepository.stopConditionEdition()
}

private fun ImageCondition.toItemBrief(context: Context, inError: Boolean): ItemBrief =
    ItemBrief(
        id = id,
        data = this,
        icon = R.drawable.ic_condition,
        name = name,
        description = "", //TODO
        inError = inError,
    )

private fun ImageCondition.toItemDescription(screenArea: Point, bitmap: Bitmap?): ImageConditionDescription =
    ImageConditionDescription(
        conditionBitmap = bitmap,
        conditionPosition = area,
        conditionDetectionArea =
            when (detectionType) {
                EXACT -> area
                IN_AREA -> detectionArea
                WHOLE_SCREEN -> Rect(0, 0, screenArea.x, screenArea.y)
                else -> null
            },
    )
