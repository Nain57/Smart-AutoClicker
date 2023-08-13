/*
 * Copyright (C) 2023 Kevin Buzeau
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

import com.buzbuz.smartautoclicker.feature.tutorial.data.StepEndCondition
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialStepData
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType


sealed class TutorialStep {

    data class ChangeFloatingUiVisibility(
        val isVisible: Boolean,
    ): TutorialStep()

    data class TutorialOverlay(
        @StringRes val tutorialInstructionsResId: Int,
        val tutorialImage: TutorialImage? = null,
        val closeType: CloseType,
    ): TutorialStep()
}

data class TutorialImage(
    @DrawableRes val tutorialImageResId: Int,
    @StringRes val tutorialImageDescResId: Int,
)

sealed class CloseType {

    object NextButton : CloseType()

    data class MonitoredViewClick(
        val type: MonitoredViewType,
    ) : CloseType()
}

internal fun TutorialStepData.toDomain(): TutorialStep =
    when (this) {
        is TutorialStepData.ChangeFloatingUiVisibility -> toDomain()
        is TutorialStepData.TutorialOverlay -> toDomain()
    }

private fun TutorialStepData.ChangeFloatingUiVisibility.toDomain(): TutorialStep.ChangeFloatingUiVisibility =
    TutorialStep.ChangeFloatingUiVisibility(
        isVisible = isVisible,
    )

private fun TutorialStepData.TutorialOverlay.toDomain(): TutorialStep =
    TutorialStep.TutorialOverlay(
        tutorialInstructionsResId = contentTextResId,
        tutorialImage = image?.let {
            TutorialImage(it.imageResId, it.imageDescResId)
        },
        closeType = stepEndCondition.toDomain(),
    )

private fun StepEndCondition.toDomain(): CloseType =
    when (this) {
        StepEndCondition.NextButton -> CloseType.NextButton
        is StepEndCondition.MonitoredViewClicked -> CloseType.MonitoredViewClick(type)
    }