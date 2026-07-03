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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.text

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepImage
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game.text.OneStillChangingTextRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem


object TextConditionsStillTextTutorial : TutorialItem {

    override fun getType(): TutorialItem.Type =
        TutorialItem.Type.TEXT_CONDITION_STILL_TEXT

    override fun getTutorialInfo(): TutorialInfo =
        TutorialInfo(
            id = getType().toTutorialId(),
            nameResId = R.string.item_title_tutorial_text_conditions_still_text,
            descResId = R.string.item_desc_tutorial_text_conditions_still_text,
        )

    override fun getTutorial(): Tutorial =
        Tutorial(
            info = getTutorialInfo(),
            subject = TutorialSubject.Game(
                instructionsResId = R.string.message_game_tutorial_text_conditions_still_text,
                scoreToReach = 20,
                durationSeconds = 10,
                rules = OneStillChangingTextRules(),
            ),
            steps = listOf(
                // Beginning, hide the overlay for now
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    newVisibility = true,
                ),
                // Open edit scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_1,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Create Event
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_2,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Select condition tab
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_3,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS,
                    ),
                ),
                // Create a new condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_4,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_CREATE,
                    ),
                ),
                // Select Text Condition type
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_5,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_TEXT,
                    ),
                ),
                // Input the text to detect
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_6,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_text_condition,
                        imageDescResId = R.string.message_tutorial_text_conditions_still_text_step_secondary_6,
                    ),
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Open the detection area selector
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_7,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.TEXT_CONDITION_DIALOG_FIELD_AREA_SELECTOR,
                    ),
                ),
                // Select the detection area
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_8,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Save condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_9,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.TEXT_CONDITION_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close condition list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_10,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Select action tab
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_11,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Create a new action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_12,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Create a new click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_13,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION,
                    ),
                ),
                // Select click location
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_14,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_FIELD_SELECT_POSITION_OR_CONDITION,
                    ),
                ),
                // Pick location
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_15,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_visible_on,
                        imageDescResId = R.string.message_tutorial_text_conditions_still_text_step_secondary_15,
                    ),
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Save click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_16,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close action list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_17,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Save event
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_18,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Save scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_19,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Play scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_20,
                    stepStartCondition = TutorialStepStartCondition.NextOverlay,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.MAIN_MENU_BUTTON_PLAY,
                    ),
                ),
                // Start game
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_21,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Game won
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_text_conditions_still_text_step_22,
                    stepStartCondition = TutorialStepStartCondition.GameWon,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
            )
        )
}
