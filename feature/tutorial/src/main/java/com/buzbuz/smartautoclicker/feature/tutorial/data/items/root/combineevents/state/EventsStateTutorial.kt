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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineevents.state

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
import com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.quickcountgame.image.FourMovingTargetsInOrderTargetRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem

object EventsStateTutorial : TutorialItem {

    override fun getType(): TutorialItem.Type =
        TutorialItem.Type.EVENTS_STATE

    override fun getTutorialInfo(): TutorialInfo =
        TutorialInfo(
            id = getType().toTutorialId(),
            nameResId = R.string.item_title_tutorial_events_state,
            descResId = R.string.item_desc_tutorial_events_state,
        )

    override fun getTutorial(): Tutorial =
        Tutorial(
            info = getTutorialInfo(),
            subject = TutorialSubject.QuickClickGame(
                instructionsResId = R.string.message_game_tutorial_events_state,
                scoreToReach = 200,
                durationSeconds = 10,
                rules = FourMovingTargetsInOrderTargetRules(),
            ),
            steps = listOf(
                // Beginning, hide the overlay for now
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    newVisibility = true,
                ),
                // Play the game or open edit scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_1,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Scenario dialog: Create Event for the blue target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_2,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Event dialog: define a name
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_3,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Image Condition dialog: select game area
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_4,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.IMAGE_CONDITION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_DIALOG_FIELD_TYPE_ITEM_IN_AREA,
                    ),
                ),
                // Event dialog: create action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_5,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Click action dialog: click on condition type
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_6,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CLICK,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_FIELD_POSITION_TYPE_ITEM_ON_CONDITION,
                    ),
                ),
                // Scenario dialog: create red
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_7,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Scenario dialog: create green
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_8,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Scenario dialog: create yellow
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_9,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Scenario dialog: save and test
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_10,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Game: play and lose, it is too slow
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_11,
                    stepStartCondition = TutorialStepStartCondition.GameLost,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Scenario dialog: explain state and all enabled
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_12,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_ITEM_FIRST_EVENT,
                    ),
                ),
                // Event dialog: blue is the first target, so we want it enabled by default, open actions
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_13,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Action type selection: blue target: pick Change event state
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_14,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.ACTION_TYPE_SELECTION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTION_TYPE_DIALOG_TOGGLE_EVENT_ACTION,
                    ),
                ),
                // Toggle Event dialog: blue target: explain toggle dialog, select changes
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_15,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.TOGGLE_EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.TOGGLE_EVENT_DIALOG_SELECT_TOGGLES,
                    ),
                ),
                // Toggle selection: blue target: enable red, disable others. Secondary show toggle ui and explain the
                // 3 values
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_16,
                    image = TutorialStepImage(
                        imageResId = R.drawable.tutorial_instructions_toggle_event,
                        imageDescResId = R.string.message_tutorial_events_state_step_16_secondary,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT_TOGGLES,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Event dialog: blue target save
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_17,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Scenario dialog: Now modify red
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_18,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_ITEM_SECOND_EVENT,
                    ),
                ),
                // Event dialog: red needs to be disabled at start, so change the initial state
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_19,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_INITIAL_STATE,
                    ),
                ),
                // Event dialog: red target: add new Change Event State action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_20,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Toggle selection: red target: enable green, disable others
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_21,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT_TOGGLES,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Scenario dialog: Now modify green
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_22,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_ITEM_THIRD_EVENT,
                    ),
                ),
                // Event dialog: green needs to be disabled at start, so change the initial state
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_23,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_INITIAL_STATE,
                    ),
                ),
                // Event dialog: green target: add new Change Event State action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_24,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Toggle selection: green target: enable yellow, disable others
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_25,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT_TOGGLES,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Scenario dialog: Now modify yellow
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_26,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_ITEM_FOURTH_EVENT,
                    ),
                ),
                // Event dialog: yellow needs to be disabled at start, so change the initial state
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_27,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_INITIAL_STATE,
                    ),
                ),
                // Event dialog: yellow target: add new Change Event State action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_28,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Toggle selection: yellow target: enable blue (as we want to loop) and disable others
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_29,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT_TOGGLES,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Scenario dialog: everything is ok save your scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_30,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCENARIO,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Game: start game (note about state reset, restart the scenario everytime we need to restart the game)
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_31,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Game won, resume what we learned
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_events_state_step_32,
                    stepStartCondition = TutorialStepStartCondition.GameWon,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
            )
        )

}