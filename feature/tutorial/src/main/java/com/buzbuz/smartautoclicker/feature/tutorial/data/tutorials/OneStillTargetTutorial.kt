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
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneStillTargetRules

internal val oneStillTargetTutorialInfo: TutorialInfo =
    TutorialInfo(
        nameResId = R.string.item_title_tutorial_1,
        descResId = R.string.item_desc_tutorial_1,
    )

internal fun newOneStillTargetTutorial(): TutorialData =
    TutorialData(
        info = oneStillTargetTutorialInfo,
        game = TutorialGameData(
            instructionsResId = R.string.message_tutorial_1_game_instructions,
            gameRules = OneStillTargetRules(150),
        ),
        steps = listOf(
            // Start screen, before first play
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_1,
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
                contentTextResId = R.string.message_tutorial_1_step_3,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.FLOATING_MENU_BUTTON_CONFIG,
                ),
            ),
            // Create a new event
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_4,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                ),
            ),
            // Select condition tab
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_5,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_CONDITIONS,
                ),
            ),
            // Create a new condition
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_6,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_CONDITION,
                ),
            ),
            // Take a screenshot
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_7,
                image = TutorialStepImage(
                    imageResId = R.drawable.ic_screenshot,
                    imageDescResId = R.string.message_tutorial_1_step_secondary_7,
                ),
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.NextButton,
            ),
            // Ensure target is captured or retry
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_8,
                image = TutorialStepImage(
                    imageResId = R.drawable.ic_cancel,
                    imageDescResId = R.string.message_tutorial_1_step_secondary_8,
                ),
                stepStartCondition = StepStartCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITION_CAPTURE_BUTTON_CAPTURE,
                ),
                stepEndCondition = StepEndCondition.NextButton,
            ),
            // Adjust screenshot
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_9,
                image = TutorialStepImage(
                    imageResId = R.drawable.tutorial_instructions_capture_sizing,
                    imageDescResId = R.string.message_tutorial_1_step_secondary_9,
                ),
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.NextButton,
            ),
            // Save condition
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_10,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITION_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Select action tab
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_11,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_ACTIONS,
                ),
            ),
            // Create a new action
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_12,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_ACTION,
                ),
            ),
            // Create a new click
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_13,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION,
                ),
            ),
            // Select click location
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_14,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CLICK_DIALOG_BUTTON_SELECT_POSITION_OR_CONDITION,
                ),
            ),
            // Pick location
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_15,
                image = TutorialStepImage(
                    imageResId = R.drawable.ic_visible_on,
                    imageDescResId = R.string.message_tutorial_1_step_secondary_15,
                ),
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.NextButton,
            ),
            // Save click
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_16,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Save event
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_17,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Save scenario
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_18,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Play scenario
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_19,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.FLOATING_MENU_BUTTON_PLAY,
                ),
            ),
            // Start game
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_20,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.NextButton,
            ),
            // Game won
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_21,
                stepStartCondition = StepStartCondition.GameWon,
                stepEndCondition = StepEndCondition.NextButton,
            ),
        )
    )