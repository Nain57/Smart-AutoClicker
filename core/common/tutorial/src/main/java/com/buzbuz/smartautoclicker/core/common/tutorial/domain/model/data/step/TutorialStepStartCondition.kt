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

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType


/**
 * Condition that must be satisfied before a [TutorialStep] is considered started.
 *
 * The engine monitors the relevant signal and calls back into the step lifecycle once the
 * condition is met.
 */
sealed class TutorialStepStartCondition {

    /** Step starts as soon as it becomes the current step — no waiting. */
    data object Immediate : TutorialStepStartCondition()

    /** Step starts after the game subject ends with a winning result. */
    data object GameWon : TutorialStepStartCondition()

    /** Step starts after the game subject ends with a losing result. */
    data object GameLost : TutorialStepStartCondition()

    /**
     * Step starts when the specified monitored UI element is clicked.
     *
     * @property type the UI element whose click event triggers this condition.
     */
    data class MonitoredViewClicked(val type: MonitoredViewType) : TutorialStepStartCondition()

    /**
     * Step starts when the specified monitored UI edit text contains the expected text.
     *
     * @property type the UI element to monitor the text of
     * @property expectedText the text expected.
     */
    data class MonitoredTextInput(val type: MonitoredViewType, val expectedText: String) : TutorialStepStartCondition()


    /**
     * Step starts when the expected overlay is pushed on top of the back stack.
     * Useful for steps that should activate only after the user opens a dialog or screen.
     *
     * @property type the type of the expected overlay.
     */
    data class MonitoredOverlayDisplayed(val type: MonitoredOverlayType) : TutorialStepStartCondition()
}