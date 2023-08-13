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
package com.buzbuz.smartautoclicker.feature.tutorial.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

import com.buzbuz.smartautoclicker.feature.tutorial.data.game.TutorialGameData
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType

internal data class TutorialData(
    val info: TutorialInfo,
    val game: TutorialGameData,
    val steps: List<TutorialStepData>,
)

internal data class TutorialInfo(
    @StringRes val nameResId: Int,
    @StringRes val descResId: Int,
)

internal sealed class TutorialStepData {

    abstract val stepStartCondition: StepStartCondition

    internal data class ChangeFloatingUiVisibility(
        override val stepStartCondition: StepStartCondition,
        val isVisible: Boolean,
    ) : TutorialStepData()

    internal data class TutorialOverlay(
        @StringRes val contentTextResId: Int,
        val image: TutorialStepImage? = null,
        override val stepStartCondition: StepStartCondition,
        val stepEndCondition: StepEndCondition,
    ) : TutorialStepData()
}

internal data class TutorialStepImage(
    @DrawableRes val imageResId: Int,
    @StringRes val imageDescResId: Int,
)

internal sealed class StepStartCondition {
    object Immediate : StepStartCondition()
    object NextOverlay : StepStartCondition()
    object GameWon : StepStartCondition()
    object GameLost : StepStartCondition()
    data class MonitoredViewClicked(val type: MonitoredViewType) : StepStartCondition()
}

internal sealed class StepEndCondition {
    object NextButton : StepEndCondition()
    data class MonitoredViewClicked(val type: MonitoredViewType) : StepEndCondition()
}