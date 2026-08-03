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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineevents.priority

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.step.TutorialStepImage
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.quickcountgame.image.TwoStillTargetsPressAnyRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem

object EventsPriorityTutorial : TutorialItem {

    override fun getType(): TutorialItem.Type =
        TutorialItem.Type.EVENTS_PRIORITY

    override fun getTutorialInfo(): TutorialInfo =
        TutorialInfo(
            id = getType().toTutorialId(),
            nameResId = R.string.item_title_tutorial_events_priority,
            descResId = R.string.item_desc_tutorial_events_priority,
        )

    override fun getTutorial(): Tutorial =
        Tutorial(
            info = getTutorialInfo(),
            subject = TutorialSubject.QuickClickGame(
                instructionsResId = R.string.message_game_tutorial_events_priority,
                scoreToReach = 200,
                durationSeconds = 10,
                rules = TwoStillTargetsPressAnyRules(
                    blueScoreIncrement = 1,
                    redScoreIncrement = 10,
                    targetsBehaviour = TwoStillTargetsPressAnyRules.TargetsBehaviour.BLUE_STILL_RED_BLINK,
                ),
            ),
            steps = listOf(
                // Beginning, hide the overlay for now
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    newVisibility = true,
                ),
                // Play the game or open edit scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_priority_step_1,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Create Event for the blue target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_priority_step_2,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Create Event for the red target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_priority_step_3,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Save scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_priority_step_4,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Play and lose, explain why (order make blue click always clicked, red is never executed)
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_priority_step_5,
                    stepStartCondition = TutorialStepStartCondition.GameLost,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Explain order and drag and drop to reorder. Reorder red before blue and save scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_priority_step_6,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Explain order and drag and drop to reorder. Reorder red before blue and save scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_priority_step_7,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_reorder,
                        imageDescResId = R.string.message_tutorial_events_priority_step_7_secondary,
                    ),
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Priority is now ok, start scenario than start the game
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_priority_step_8,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Game won
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_priority_step_9,
                    stepStartCondition = TutorialStepStartCondition.GameWon,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
            )
        )

}