/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials

import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.StepEndCondition
import com.buzbuz.smartautoclicker.feature.tutorial.data.StepStartCondition
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialData
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialStepData
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialStepImage
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.TutorialGameData
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoStillTargetsPressWhenBothVisibleRules

internal val twoStillTargetsPressWhenBothVisibleTutorialInfo: TutorialInfo =
    TutorialInfo(
        nameResId = R.string.item_title_tutorial_3,
        descResId = R.string.item_desc_tutorial_3,
    )

internal fun newTwoStillTargetsPressWhenBothVisibleTutorial(): TutorialData =
    TutorialData(
        info = twoStillTargetsPressWhenBothVisibleTutorialInfo,
        game = TutorialGameData(
            instructionsResId = R.string.message_tutorial_3_game_instructions,
            gameRules = TwoStillTargetsPressWhenBothVisibleRules(30),
        ),
        steps = listOf(
            // Start screen, before first play
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_1,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.NextButton,
            ),
            // First play lost, make floating menu visible
            TutorialStepData.ChangeFloatingUiVisibility(
                stepStartCondition = StepStartCondition.GameLost,
                isVisible = true,
            ),
            // First play lost, open edit scenario
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_3,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.MAIN_MENU_BUTTON_CONFIG,
                ),
            ),
            // Open first Event
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_4,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.SCENARIO_DIALOG_ITEM_FIRST_EVENT,
                ),
            ),
            // Click on Condition operator AND
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_5,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_FIELD_OPERATOR_ITEM_AND,
                ),
            ),
            // Select condition field
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_6,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS,
                ),
            ),
            // Create a new condition
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_7,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_CREATE,
                ),
            ),
            // Take a screenshot
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_8,
                image = TutorialStepImage(
                    imageResId = R.drawable.ic_capture,
                    imageDescResId = R.string.message_tutorial_3_step_secondary_8,
                ),
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.NextButton,
            ),
            // Ensure target is captured or retry
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_9,
                image = TutorialStepImage(
                    imageResId = R.drawable.ic_cancel,
                    imageDescResId = R.string.message_tutorial_3_step_secondary_9,
                ),
                stepStartCondition = StepStartCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITION_CAPTURE_MENU_BUTTON_CAPTURE,
                ),
                stepEndCondition = StepEndCondition.NextButton,
            ),
            // Save condition
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_10,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITION_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Close condition list
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_11,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITION_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Select action field
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_12,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                ),
            ),
            // Close Event List
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_13,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_SAVE,
                ),
            ),
            // Save Event
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_14,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Save scenario
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_15,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Play scenario
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_16,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.MAIN_MENU_BUTTON_PLAY,
                ),
            ),
            // Game won
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_3_step_17,
                stepStartCondition = StepStartCondition.GameWon,
                stepEndCondition = StepEndCondition.NextButton,
            ),
        )
    )