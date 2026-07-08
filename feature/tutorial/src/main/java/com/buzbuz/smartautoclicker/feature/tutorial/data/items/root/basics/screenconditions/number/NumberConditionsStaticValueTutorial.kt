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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.number

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepImage
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game.number.OneStillChangingNumberClickWhenOverRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem

object NumberConditionsStaticValueTutorial : TutorialItem {

    override fun getType(): TutorialItem.Type =
        TutorialItem.Type.NUMBER_CONDITION_STATIC_VALUE

    override fun getTutorialInfo(): TutorialInfo =
        TutorialInfo(
            id = getType().toTutorialId(),
            nameResId = R.string.item_title_tutorial_number_conditions_static_value,
            descResId = R.string.item_desc_tutorial_number_conditions_static_value,
        )

    override fun getTutorial(): Tutorial =
        Tutorial(
            info = getTutorialInfo(),
            subject = TutorialSubject.Game(
                instructionsResId = R.string.message_game_tutorial_number_conditions_static_value,
                scoreToReach = 30,
                durationSeconds = 20,
                rules = OneStillChangingNumberClickWhenOverRules(
                    validWhenClickIsOver = 9000,
                    maxValue = 18000,
                ),
            ),
            steps = listOf(
                // Beginning, hide the overlay for now
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    newVisibility = true,
                ),
                // Open edit scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_1,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Create Event
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_2,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Select condition tab
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_3,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS,
                    ),
                ),
                // Create a new condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_4,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCREEN_CONDITIONS_BRIEF_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_CREATE,
                    ),
                ),
                // Select Text Condition type
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_5,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCREEN_CONDITION_TYPE_SELECTION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_NUMBER,
                    ),
                ),
                // Input the text to detect
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_6,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_counter_reached,
                        imageDescResId = R.string.message_tutorial_number_conditions_static_value_step_secondary_6,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.NUMBER_CONDITION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Click on the operator dropdown
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_7,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        type = MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_OPERATOR_DROPDOWN,
                    ),
                ),
                // Select Greater Item
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_8,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        type = MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_OPERATOR_DROPDOWN_ITEM_GREATER,
                    ),
                ),
                // Select Greater Item
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_9,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Open the detection area selector
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_10,
                    stepStartCondition = TutorialStepStartCondition.MonitoredNumberInput(
                        type = MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_VALUE_TO_DETECT,
                        expectedNumber = 9000.0,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.NUMBER_CONDITION_DIALOG_FIELD_AREA_SELECTOR,
                    ),
                ),
                // Select the detection area
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_11,
                    image = TutorialStepImage(
                        imageResId = R.drawable.tutorial_instructions_number_capture_sizing,
                        imageDescResId = R.string.message_tutorial_number_conditions_static_value_step_secondary_11,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CONDITION_AREA_SELECTOR_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Save condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_12,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.NUMBER_CONDITION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.NUMBER_CONDITION_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close condition list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_13,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCREEN_CONDITIONS_BRIEF_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Select action tab
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_14,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Create a new action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_15,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SMART_ACTIONS_BRIEF_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Create a new click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_16,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.ACTION_TYPE_SELECTION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION,
                    ),
                ),
                // Select position type
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_17,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CLICK,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_FIELD_SELECT_POSITION_OR_CONDITION,
                    ),
                ),
                // Select a position for the click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_18,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_visible_on,
                        imageDescResId = R.string.message_tutorial_screen_condition_still_target_step_secondary_17,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CLICK_POSITION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Save click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_19,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CLICK,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close action list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_20,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SMART_ACTIONS_BRIEF_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Save event
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_21,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Save scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_22,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Play scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_23,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.MAIN_MENU_BUTTON_PLAY,
                    ),
                ),
                // Start game
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_24,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Game won
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_number_conditions_static_value_step_25,
                    stepStartCondition = TutorialStepStartCondition.GameWon,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),

            )
        )
}