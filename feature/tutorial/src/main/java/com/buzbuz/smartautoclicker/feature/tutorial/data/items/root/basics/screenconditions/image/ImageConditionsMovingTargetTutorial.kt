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
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game.image.OneMovingTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem


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
                // Beginning, hide the overlay for now
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    newVisibility = false,
                ),
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
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
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
                // Create a new condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_6,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_CREATE,
                    ),
                ),
                // Select Image Condition type
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_7,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_IMAGE,
                    ),
                ),
                // Take a screenshot
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_8,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_capture,
                        imageDescResId = R.string.message_tutorial_screen_condition_moving_target_step_secondary_8,
                    ),
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Ensure target is captured or retry
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_9,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_cancel,
                        imageDescResId = R.string.message_tutorial_screen_condition_moving_target_step_secondary_9,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_CAPTURE_MENU_BUTTON_CAPTURE,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Adjust screenshot
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_10,
                    image = TutorialStepImage(
                        imageResId = R.drawable.tutorial_instructions_capture_sizing,
                        imageDescResId = R.string.message_tutorial_screen_condition_moving_target_step_secondary_10,
                    ),
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Click on In area Detection Type
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_11,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_warning,
                        imageDescResId = R.string.message_tutorial_screen_condition_moving_target_step_secondary_11,
                    ),
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_DIALOG_FIELD_TYPE_ITEM_IN_AREA,
                    ),
                ),
                // Click on area selector
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_12,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_DIALOG_FIELD_AREA_SELECTOR,
                    ),
                ),
                // Select Area
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_13,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Save condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_14,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close condition list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_15,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Select action field
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_16,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Create a new action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_17,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Create a new click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_18,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION,
                    ),
                ),
                // Select position type Condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_19,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_FIELD_POSITION_TYPE_ITEM_ON_CONDITION,
                    ),
                ),
                // Select a condition for the click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_20,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_FIELD_SELECT_POSITION_OR_CONDITION,
                    ),
                ),
                // Pick blue character
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_21,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITION_SELECTOR_DIALOG_ITEM_FIRST,
                    ),
                ),
                // Save Click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_22,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close action list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_23,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Save Event
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_24,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Save Scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_25,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Start detection and game
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_26,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Game won
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_screen_condition_moving_target_step_27,
                    stepStartCondition = TutorialStepStartCondition.GameWon,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
            )
        )

}