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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay

import android.app.Application
import android.graphics.Rect

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.display.DisplayMetrics
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialStep
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.CloseType

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class TutorialOverlayViewModel(application: Application) : AndroidViewModel(application) {

    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()
    private val tutorialRepository: TutorialRepository = TutorialRepository.getTutorialRepository(application)
    private val displayMetrics: DisplayMetrics = DisplayMetrics.getInstance(application)

    val uiState: Flow<UiTutorialOverlayState?> = tutorialRepository.activeStep
        .map { step -> if (step is TutorialStep.TutorialOverlay) step else null }
        .flatMapLatest { tutorialOverlay ->
            tutorialOverlay ?: return@flatMapLatest flowOf(null)

            when (tutorialOverlay.closeType) {
                CloseType.NextButton -> flowOf(
                    UiTutorialOverlayState(
                        instructionsResId = tutorialOverlay.tutorialInstructionsResId,
                        image = tutorialOverlay.getImage(),
                        exitButton = TutorialExitButton.Next,
                    )
                )

                is CloseType.MonitoredViewClick -> {
                    monitoredViewsManager.getViewPosition(tutorialOverlay.closeType.type)?.map { position ->
                        UiTutorialOverlayState(
                            instructionsResId = tutorialOverlay.tutorialInstructionsResId,
                            image = tutorialOverlay.getImage(),
                            exitButton = TutorialExitButton.MonitoredView(tutorialOverlay.closeType.type, position),
                            isDisplayedInTopHalf = position.centerY() > displayMetrics.screenSize.y / 2,
                        )
                    } ?: flowOf(UiTutorialOverlayState(tutorialOverlay.tutorialInstructionsResId))
                }
            }
        }

    fun toNextTutorialStep() {
        tutorialRepository.nextTutorialStep()
    }

    fun toLastTutorialStep() {
        tutorialRepository.lastTutorialStep()
    }
}

data class UiTutorialOverlayState(
    @StringRes val instructionsResId: Int,
    val image: UiTutorialImage? = null,
    val exitButton: TutorialExitButton? = null,
    val isDisplayedInTopHalf: Boolean = true,
)

data class UiTutorialImage(
    @DrawableRes val imageResId: Int,
    @StringRes val imageDescResId: Int,
)

sealed class TutorialExitButton {
    object Next : TutorialExitButton()
    data class MonitoredView(val type: MonitoredViewType, val position: Rect) : TutorialExitButton()
}

private fun TutorialStep.TutorialOverlay.getImage() : UiTutorialImage? =
    tutorialImage?.let {
        UiTutorialImage(it.tutorialImageResId, it.tutorialImageDescResId)
    }
