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
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneMovingTargetRules

internal val oneMovingTargetTutorialInfo: TutorialInfo =
    TutorialInfo(
        nameResId = R.string.item_title_tutorial_2,
        descResId = R.string.item_desc_tutorial_2,
    )

internal fun newOneMovingTargetTutorial(): TutorialData =
    TutorialData(
        info = oneMovingTargetTutorialInfo,
        game = TutorialGameData(
            instructionsResId = R.string.message_tutorial_2_game_instructions,
            gameRules = OneMovingTargetRules(50),
        ),
        steps = listOf(
            // Start screen, before first play
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_1,
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
                contentTextResId = R.string.message_tutorial_2_step_3,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.FLOATING_MENU_BUTTON_CONFIG,
                ),
            ),
            // Open first Event
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_4,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.SCENARIO_DIALOG_ITEM_FIRST_EVENT,
                ),
            ),
            // Select condition tab
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_5,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_CONDITIONS,
                ),
            ),
            // Open first Condition
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_6,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_CONDITION,
                ),
            ),
            // Click on Detection Type
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_7,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITION_DIALOG_DROPDOWN_DETECTION_TYPE,
                ),
            ),
            // Click on Whole Screen
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_8,
                image = TutorialStepImage(
                    imageResId = R.drawable.ic_warning,
                    imageDescResId = R.string.message_tutorial_2_step_secondary_8,
                ),
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITION_DIALOG_DROPDOWN_ITEM_WHOLE_SCREEN,
                ),
            ),
            // Save condition
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_9,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITION_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Select action tab
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_10,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_ACTIONS,
                ),
            ),
            // Click on first action
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_11,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_ACTION,
                ),
            ),
            // Click on Position/Condition dropdown
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_12,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CLICK_DIALOG_DROPDOWN_CLICK_ON,
                ),
            ),
            // Select Condition in dropdown
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_13,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CLICK_DIALOG_DROPDOWN_ITEM_CLICK_ON_CONDITION,
                ),
            ),
            // Save Click
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_14,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CLICK_DIALOG_BUTTON_SELECT_POSITION_OR_CONDITION,
                ),
            ),
            // Pick blue character
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_15,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CONDITION_SELECTOR_DIALOG_ITEM_FIRST,
                ),
            ),
            // Save Click
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_16,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Save Event
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_17,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Save Scenario
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_18,
                stepStartCondition = StepStartCondition.NextOverlay,
                stepEndCondition = StepEndCondition.MonitoredViewClicked(
                    MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                ),
            ),
            // Start detection and game
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_19,
                stepStartCondition = StepStartCondition.Immediate,
                stepEndCondition = StepEndCondition.NextButton,
            ),
            // Game won
            TutorialStepData.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_2_step_20,
                stepStartCondition = StepStartCondition.GameWon,
                stepEndCondition = StepEndCondition.NextButton,
            ),
        )
    )