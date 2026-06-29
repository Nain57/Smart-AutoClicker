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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.state

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep

/**
 * Lifecycle state of the tutorial engine, emitted on [TutorialRepository.tutorialState].
 *
 * Feature UIs observe this flow to show or hide tutorial overlays and react to step transitions.
 */
sealed interface TutorialState {

    /** The engine is idle — no tutorial is running. */
    data object Stopped : TutorialState

    /**
     * A tutorial is currently running.
     *
     * @property tutorial the tutorial definition that was started.
     * @property isCompleted `true` once the user has finished all steps.
     * @property isCurrentStepStarted `true` once the current step's [TutorialStepStartCondition]
     *   has been satisfied and the step is actively shown to the user.
     * @property currentStep the step currently being executed, or `null` briefly during transitions.
     */
    data class Started(
        val tutorial: Tutorial,
        val isCompleted: Boolean,
        val isCurrentStepStarted: Boolean,
        val currentStep: TutorialStep?,
    ) : TutorialState

}

