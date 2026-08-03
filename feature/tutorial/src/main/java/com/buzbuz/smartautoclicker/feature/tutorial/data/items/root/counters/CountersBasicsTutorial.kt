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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.counters

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
import com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.quickcountgame.image.CountBlueValidateRedRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem

object CountersBasicsTutorial : TutorialItem {

    override fun getType(): TutorialItem.Type =
        TutorialItem.Type.COUNTERS_BASICS

    override fun getTutorialInfo(): TutorialInfo =
        TutorialInfo(
            id = getType().toTutorialId(),
            nameResId = R.string.item_title_tutorial_counters_basics,
            descResId = R.string.item_desc_tutorial_counters_basics,
        )

    override fun getTutorial(): Tutorial =
        Tutorial(
            info = getTutorialInfo(),
            subject = TutorialSubject.QuickClickGame(
                instructionsResId = R.string.message_game_tutorial_counters_basics,
                scoreToReach = 70,
                durationSeconds = 10,
                rules = CountBlueValidateRedRules(),
            ),
            steps = listOf(
                // Beginning, hide the overlay for now
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    newVisibility = true,
                ),
                // Play the game or open edit scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_1,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Scenario dialog: Create Event for the blue target that clicks on it
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_2,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Event dialog: Add a condition for the blue target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_3,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS,
                    ),
                ),
                // Event dialog: Add actions for the blue target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_4,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Action list: create click on blue target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_5,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SMART_ACTIONS_BRIEF_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Click dialog: configure to click on the blue target and save
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_6,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CLICK,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Action list: create set counter action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_7,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SMART_ACTIONS_BRIEF_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Action list: pick change counter action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_8,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.ACTION_TYPE_SELECTION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTION_TYPE_DIALOG_COUNTER_ACTION,
                    ),
                ),
                // Change Counter dialog: Click on select a counter
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_9,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CHANGE_COUNTER,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.COUNTER_ACTION_DIALOG_FIELD_SELECT_COUNTER,
                    ),
                ),
                // Counter Selection dialog: Create a counter, give it a name, keep starting at 0 and save. Select it
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_10,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.COUNTER_SELECTION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.COUNTER_SELECTION_DIALOG_BUTTON_CREATE,
                    ),
                ),
                // Change Counter dialog: Explain set operator on counter (=, +, -). Ask to do +1
                // Image shows selection between static & other counter operand.
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_11,
                    image = TutorialStepImage(
                        imageResId = R.drawable.tutorial_instructions_change_counter_value_type,
                        imageDescResId = R.string.message_tutorial_counters_basics_step_11_secondary,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CHANGE_COUNTER,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Event dialog: Event is ok, we click and count the number of clicks
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_12,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Scenario dialog: Need to act when counter = 10, click on trigger event tab
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_13,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_TRIGGER_EVENT_TAB,
                    ),
                ),
                // Scenario dialog: Create a new TriggerEvent
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_14,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Event dialog: First, create a new Counter Reached condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_15,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS,
                    ),
                ),
                // Condition type dialog: Pick counter reached
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_16,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.TRIGGER_CONDITION_TYPE_SELECTION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.TRIGGER_CONDITION_TYPE_ON_COUNTER_REACHED,
                    ),
                ),
                // Counter reached dialog: Select the blue target counter created before
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_17,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.COUNTER_REACHED_CONDITION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.COUNTER_REACHED_DIALOG_FIELD_COUNTER_SELECTION,
                    ),
                ),
                // Counter reached dialog: explain comparison operation. Ask to verify = 10
                // Image shows selection between static & other counter value.
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_18,
                    image = TutorialStepImage(
                        imageResId = R.drawable.tutorial_instructions_change_counter_value_type,
                        imageDescResId = R.string.message_tutorial_counters_basics_step_18_secondary,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.COUNTER_REACHED_CONDITION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Event dialog: Add actions for the red target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_19,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Action list: create click on red target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_20,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SMART_ACTIONS_BRIEF_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Click dialog: configure to click on the red target and save. As this is trigger event, can't
                // click on condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_21,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CLICK,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Action list: create set counter action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_22,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SMART_ACTIONS_BRIEF_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Change Counter dialog: Click on select a counter and pick the blue target counter
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_23,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CHANGE_COUNTER,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.COUNTER_ACTION_DIALOG_FIELD_SELECT_COUNTER,
                    ),
                ),
                // Change Counter dialog: Ask to do "= 0" to reset the counter and save
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_24,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CHANGE_COUNTER,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Event dialog: Trigger event is ok, triggers when 10, clicks on red and reset
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_25,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Scenario dialog: scenario is complete, when click & count, then reset; save it.
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_26,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Game: Start the scenario and start the game
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_27,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Game won: resume what we learned
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_counters_basics_step_28,
                    stepStartCondition = TutorialStepStartCondition.GameWon,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
            )
        )

}