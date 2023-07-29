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
package com.buzbuz.smartautoclicker.feature.tutorial.data

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.TutorialGameData
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneMovingTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.OneStillTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoMovingTargetsPressInOrderRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoStillTargetsPressWhenBothVisibleRules
import com.buzbuz.smartautoclicker.feature.tutorial.data.game.rules.TwoStillTargetsPressWhenOneVisibleRules
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType

internal object TutorialDataSource {

    val tutorials: List<TutorialData> = listOf(
        TutorialData(
            nameResId = R.string.item_title_game_1,
            descResId = R.string.item_desc_game_1,
            game = TutorialGameData(
                instructionsResId = R.string.message_tutorial_instructions_1,
                gameRules = OneStillTargetRules(10),
            ),
            steps = listOf(
                // Start screen, before first play
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = true,
                    stepStartCondition = StepStartCondition.Immediate,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
                // First play lost, open edit scenario
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.GameLost,
                    stepEndCondition = StepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.FLOATING_MENU_BUTTON_CONFIG,
                    ),
                ),
                // Create a new event
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Select condition tab
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_CONDITIONS,
                    ),
                ),
                // Create a new condition
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.Immediate,
                    stepEndCondition = StepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_CONDITION,
                    ),
                ),
                // Take a screenshot
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
                // Adjust screenshot
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITION_CAPTURE_BUTTON_CAPTURE,
                    ),
                    stepEndCondition = StepEndCondition.NextButton,
                ),
                // Save condition
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
                // Select action tab
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_TAB_BUTTON_ACTIONS,
                    ),
                ),
                // Create a new action
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.Immediate,
                    stepEndCondition = StepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Create a new click
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION,
                    ),
                ),
                // Select click location
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_SELECT_POSITION_BUTTON,
                    ),
                ),
                // Pick location
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
                // Save click
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.NextButton
                ),
                // Save event
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.NextButton
                ),
                // Save scenario
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.NextButton
                ),
                // Play scenario
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.NextOverlay,
                    stepEndCondition = StepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.FLOATING_MENU_BUTTON_PLAY,
                    ),
                ),
                // Start game
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.Immediate,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
                // Game won
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = false,
                    stepStartCondition = StepStartCondition.GameWon,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
            )
        ),

        TutorialData(
            nameResId = R.string.item_title_game_2,
            descResId = R.string.item_desc_game_2,
            game = TutorialGameData(
                instructionsResId = R.string.message_tutorial_instructions_1,
                gameRules = OneMovingTargetRules(10),
            ),
            steps = listOf(
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    stepStartCondition = StepStartCondition.Immediate,
                    hideFloatingUi = true,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
            )
        ),

        TutorialData(
            nameResId = R.string.item_title_game_3,
            descResId = R.string.item_desc_game_3,
            game = TutorialGameData(
                instructionsResId = R.string.message_tutorial_instructions_1,
                gameRules = TwoStillTargetsPressWhenBothVisibleRules(10),
            ),
            steps = listOf(
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = true,
                    stepStartCondition = StepStartCondition.Immediate,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
            )
        ),

        TutorialData(
            nameResId = R.string.item_title_game_4,
            descResId = R.string.item_desc_game_4,
            game = TutorialGameData(
                instructionsResId = R.string.message_tutorial_instructions_1,
                gameRules = TwoStillTargetsPressWhenOneVisibleRules(10),
            ),
            steps = listOf(
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = true,
                    stepStartCondition = StepStartCondition.Immediate,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
            )
        ),

        TutorialData(
            nameResId = R.string.item_title_game_5,
            descResId = R.string.item_desc_game_5,
            game = TutorialGameData(
                instructionsResId = R.string.message_tutorial_instructions_1,
                gameRules = TwoMovingTargetsPressInOrderRules(10),
            ),
            steps = listOf(
                TutorialStepData(
                    contentTextResId = R.string.message_tutorial_instructions_1,
                    hideFloatingUi = true,
                    stepStartCondition = StepStartCondition.Immediate,
                    stepEndCondition = StepEndCondition.NextButton,
                ),
            )
        ),
    )
}