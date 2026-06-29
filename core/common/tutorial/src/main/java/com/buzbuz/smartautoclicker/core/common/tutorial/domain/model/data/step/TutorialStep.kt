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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step

import androidx.annotation.StringRes


/**
 * A single step in a tutorial's step sequence.
 *
 * Each step has a [stepStartCondition] that gates when the step becomes active and a
 * [stepEndCondition] that determines when the engine advances to the next step. The concrete
 * subtype defines what the UI should render while the step is active.
 */
sealed class TutorialStep {

    /** Condition that must be met before this step is considered started. */
    abstract val stepStartCondition: TutorialStepStartCondition

    /** Condition that must be met to advance to the next step. */
    abstract val stepEndCondition: TutorialStepEndCondition

    /**
     * A step that toggles the visibility of the floating UI overlay.
     *
     * Always ends immediately ([TutorialStepEndCondition.Immediate]) so the engine moves on
     * without waiting for any user action.
     *
     * @property newVisibility `true` to show the overlay, `false` to hide it.
     */
    data class ChangeFloatingUiVisibility(
        override val stepStartCondition: TutorialStepStartCondition,
        override val stepEndCondition: TutorialStepEndCondition.OverlayStackVisibilityChanged = TutorialStepEndCondition.OverlayStackVisibilityChanged,
        val newVisibility: Boolean,
    ) : TutorialStep()

    /**
     * A step that displays an instructional overlay to the user.
     *
     * @property contentTextResId string resource for the main instructional text shown in the overlay.
     * @property image optional image shown alongside the text, or `null` if text-only.
     */
    data class TutorialOverlay(
        override val stepStartCondition: TutorialStepStartCondition,
        override val stepEndCondition: TutorialStepEndCondition,
        @field:StringRes val contentTextResId: Int,
        val image: TutorialStepImage? = null,
    ) : TutorialStep()
}
