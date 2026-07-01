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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepImage
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game.OneMovingTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.TutorialItem


object ImageConditionsMovingTargetTutorial : TutorialItem {

    override fun getType(): TutorialItem.Type =
        TutorialItem.Type.IMAGE_DETECTION_MOVING_TARGET

    override fun getTutorialInfo(): TutorialInfo =
         TutorialInfo(
            id = getType().toTutorialId(),
            nameResId = R.string.item_title_tutorial_screen_condition_moving_target,
            descResId = R.string.item_desc_tutorial_screen_condition_moving_target,
        )

    override fun getTutorial(): Tutorial =
        Tutorial(
            info = getTutorialInfo(),
            subject = TutorialSubject.Game(
                instructionsResId = R.string.message_game_tutorial_screen_condition_moving_target,
                scoreToReach = 30,
                durationSeconds = 10,
                rules = OneMovingTargetRules(),
            ),
            steps = listOf(
                // Start screen, before first play
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_1,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // First play lost, make floating menu visible
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.GameLost,
                    newVisibility = true,
                ),
                // First play lost, open edit scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_3,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.MAIN_MENU_BUTTON_CONFIG,
                    ),
                ),
                // Open first Event
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_4,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_ITEM_FIRST_EVENT,
                    ),
                ),
                // Select condition tab
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_5,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS,
                    ),
                ),
                // Open first Condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_6,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_FIRST_ITEM,
                    ),
                ),
                // Click on Whole Screen Detection Type
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_7,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_warning,
                        imageDescResId = R.string.message_tutorial_screen_condition_moving_target_step_secondary_7,
                    ),
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITION_DIALOG_FIELD_TYPE_ITEM_WHOLE_SCREEN,
                    ),
                ),
                // Save condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_8,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITION_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close condition list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_9,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Select action field
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_10,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Click on first action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_11,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_FIRST_ITEM,
                    ),
                ),
                // Select position type Condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_12,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_FIELD_POSITION_TYPE_ITEM_ON_CONDITION,
                    ),
                ),
                // Select a condition for the click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_13,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_FIELD_SELECT_POSITION_OR_CONDITION,
                    ),
                ),
                // Pick blue character
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_14,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITION_SELECTOR_DIALOG_ITEM_FIRST,
                    ),
                ),
                // Save Click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_15,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close action list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_16,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Save Event
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_17,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Save Scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_18,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Start detection and game
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_19,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Game won
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_20,
                    stepStartCondition = TutorialStepStartCondition.GameWon,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
            )
        )

}