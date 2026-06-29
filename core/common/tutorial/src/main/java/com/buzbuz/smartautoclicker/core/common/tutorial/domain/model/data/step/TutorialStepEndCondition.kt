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

import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType

/**
 * Condition that must be satisfied to advance past the current [TutorialStep].
 *
 * The engine monitors the relevant signal and moves to the next step once the condition is met.
 */
sealed class TutorialStepEndCondition {

    /** Step ends immediately after it starts — the engine advances without any user action. */
    data object Immediate : TutorialStepEndCondition()

    /** Step ends once the overlay backstack visibility has changed. */
    data object OverlayStackVisibilityChanged : TutorialStepEndCondition()

    /**
     * Step ends when the user explicitly taps the "Next" button in the tutorial overlay.
     *
     * The engine exposes `onNextStepButtonPressed()` for the feature layer to call on that tap.
     */
    data object NextButton : TutorialStepEndCondition()

    /**
     * Step ends when the specified monitored UI element is clicked.
     *
     * @property type the UI element whose click event triggers advancement to the next step.
     */
    data class MonitoredViewClicked(val type: MonitoredViewType) : TutorialStepEndCondition()
}