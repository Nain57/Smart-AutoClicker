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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.click

import android.graphics.Bitmap
import android.graphics.Point
import androidx.lifecycle.ViewModel
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.display.config.DisplayConfigManager
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.ConditionOperator
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ScreenEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import javax.inject.Inject

class ClickOffsetViewModel @Inject constructor(
    private val repository: IRepository,
    private val editionRepository: EditionRepository,
    private val displayConfigManager: DisplayConfigManager,
) : ViewModel() {

    /** The ImageEvent being edited by the user. */
    private val editedEvent: Flow<ScreenEvent> = editionRepository.editionState.editedEventState
        .mapNotNull { event -> event.value }
        .filterIsInstance<ScreenEvent>()

    /** The ImageConditions being edited by the user. */
    private val editedImageConditions: Flow<List<ImageCondition>> =
        editionRepository.editionState.editedEventImageConditionsState
            .mapNotNull { it.value }

    /** The Action currently configured by the user. */
    private val configuredClick = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Click>()

    private val conditionToShow: Flow<ImageCondition?> =
        combine(editedEvent, editedImageConditions, configuredClick) { event, imageConditions, click ->
            if (!click.haveDeterminedCondition(event.conditionOperator)) null
            else imageConditions.getImageConditionFromId(click.clickOnConditionId)
        }

    private val initialClickOffset: Flow<Point> = configuredClick
        .map { click -> click.clickOffset ?: Point(0, 0) }
        .take(1)

    private val userClickOffset: MutableStateFlow<ClickOffsetState?> =
        MutableStateFlow(null)

    val clickOffset: Flow<ClickOffsetState> = combine(initialClickOffset, userClickOffset) { initial, user ->
        if (user == null) ClickOffsetState(initial, ClickOffsetUpdateType.INITIAL)
        else ClickOffsetState(user.offset, user.updateFrom)
    }

    val conditionImage: Flow<Bitmap?> = conditionToShow
        .map { imageCondition -> imageCondition?.let { repository.getConditionBitmap(it) } }

    fun getOffsetMaxBoundsX(): IntRange =
        displayConfigManager.displayConfig.let { displayConfig ->
            IntRange(-displayConfig.sizePx.x / 2, displayConfig.sizePx.x / 2)
        }

    fun getOffsetMaxBoundsY(): IntRange =
        displayConfigManager.displayConfig.let { displayConfig ->
            IntRange(-displayConfig.sizePx.y / 2, displayConfig.sizePx.y / 2)
        }

    fun setClickOffset(offset: Point, from: ClickOffsetUpdateType) {
        userClickOffset.value = ClickOffsetState(offset, from)
    }

    fun setClickOffsetX(offsetX: Int, from: ClickOffsetUpdateType) {
        val currentOffset = getCurrentOffset()
        userClickOffset.value = ClickOffsetState(Point(offsetX, currentOffset.y), from)
    }

    fun setClickOffsetY(offsetY: Int, from: ClickOffsetUpdateType) {
        val currentOffset = getCurrentOffset()
        userClickOffset.value = ClickOffsetState(Point(currentOffset.x, offsetY), from)
    }

    fun saveChanges() {
        val clickOffset = userClickOffset.value?.offset ?: return

        editionRepository.editionState.getEditedAction<Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(clickOffset = clickOffset))
        }
    }

    private fun getCurrentOffset(): Point =
        userClickOffset.value?.offset
            ?: editionRepository.editionState.getEditedAction<Click>()?.clickOffset
            ?: Point(0, 0)

    private fun Click.haveDeterminedCondition(@ConditionOperator conditionOperator: Int): Boolean =
        positionType == Click.PositionType.ON_DETECTED_CONDITION
                && conditionOperator == AND
                && clickOnConditionId != null

    private fun List<ImageCondition>.getImageConditionFromId(id: Identifier?): ImageCondition? =
        id?.let { identifier -> find { imageCondition -> imageCondition.id == identifier } }
}

data class ClickOffsetState(
    val offset: Point,
    val updateFrom: ClickOffsetUpdateType,
)

enum class ClickOffsetUpdateType{
    INITIAL,
    TEXT_INPUT,
    VIEW,
}
