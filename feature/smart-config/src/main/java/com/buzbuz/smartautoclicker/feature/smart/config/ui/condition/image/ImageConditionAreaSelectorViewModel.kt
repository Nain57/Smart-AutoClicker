
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.image

import android.graphics.Rect

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.IN_AREA
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

import javax.inject.Inject

class ImageConditionAreaSelectorViewModel @Inject constructor(
    editionRepository: EditionRepository,
) : ViewModel()  {


    /** The condition being configured by the user. */
    private val configuredCondition = editionRepository.editionState.editedImageConditionState
        .mapNotNull { it.value }

    /** The position at which the selector should be initialized. */
    val initialArea: Flow<SelectorUiState> = configuredCondition
        .mapNotNull { condition ->
            if (condition.detectionType != IN_AREA) null
            else SelectorUiState(
                initialArea = condition.detectionArea ?: condition.area,
                minimalArea = condition.area,
            )
        }
}

data class SelectorUiState(
    val initialArea: Rect,
    val minimalArea: Rect,
)