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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.triggerconditions

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem

object TimerReachedConditionTutorial : TutorialItem {

    override fun getType(): TutorialItem.Type = TutorialItem.Type.TIMER_REACHED_CONDITION

    override fun getTutorialInfo(): TutorialInfo =
        TutorialInfo(
            id = getType().toTutorialId(),
            nameResId = R.string.item_title_tutorial_timer_reached_conditions,
            descResId = R.string.item_desc_tutorial_timer_reached_conditions,
        )

    override fun getTutorial(): Tutorial =
        Tutorial(
            info = getTutorialInfo(),
            subject = TutorialSubject.TimingGame(
                instructionsResId = R.string.message_game_tutorial_timer_reached_conditions,
                clickCount = 5,
                frequencyMs = 1000,
                targetTotalDiffMs = 100,
            ),
            steps = listOf(
                // Beginning
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    newVisibility = true,
                ),
                // Open edit scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_1,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Click on Trigger Event tab
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_2,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_TRIGGER_EVENT_TAB,
                    ),
                ),
                // Create a new condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_3,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Select condition tab
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_4,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS,
                    ),
                ),
                // Create a new condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_5,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.TRIGGER_CONDITION_LIST,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.TRIGGER_CONDITION_LIST_DIALOG_BUTTON_CREATE,
                    ),
                ),
                // Select Timer Condition type
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_6,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.TRIGGER_CONDITION_TYPE_SELECTION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_TIMER_REACHED,
                    ),
                ),
                // Talk about timing and prompt user to write 1000 ms
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_7,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.TIMER_REACHED_CONDITION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Change restart value
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_8,
                    stepStartCondition = TutorialStepStartCondition.MonitoredNumberInput(
                        type = MonitoredViewType.TIMER_REACHED_CONDITION_FIELD_AFTER,
                        expectedNumber = 1000.0,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.TIMER_REACHED_CONDITION_FIELD_RESTART,
                    ),
                ),
                // Save condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_9,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.TIMER_REACHED_CONDITION_BUTTON_SAVE,
                    ),
                ),
                // Close condition list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_10,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.TRIGGER_CONDITION_LIST,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.TRIGGER_CONDITION_LIST_DIALOG_BUTTON_CLOSE,
                    ),
                ),
                // Open actions list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_11,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Create click action and return
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_12,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SMART_ACTIONS_BRIEF_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Save scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_13,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Start game
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_timer_reached_conditions_step_14,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
            )
        )
}