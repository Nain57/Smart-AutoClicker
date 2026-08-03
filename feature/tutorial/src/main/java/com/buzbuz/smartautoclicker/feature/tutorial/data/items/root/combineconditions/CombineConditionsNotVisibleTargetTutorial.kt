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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineconditions

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.tutorial.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredOverlayType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.quickcountgame.image.TwoStillTargetsPressWhenOneVisibleRules
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem

object CombineConditionsNotVisibleTargetTutorial : TutorialItem {

    override fun getType(): TutorialItem.Type =
        TutorialItem.Type.COMBINE_CONDITIONS_NOT_VISIBLE

    override fun getTutorialInfo(): TutorialInfo =
        TutorialInfo(
            id = getType().toTutorialId(),
            nameResId = R.string.item_title_tutorial_combine_conditions_not_visible_target,
            descResId = R.string.item_desc_tutorial_combine_conditions_not_visible_target,
        )

    override fun getTutorial(): Tutorial =
        Tutorial(
            info = getTutorialInfo(),
            subject = TutorialSubject.QuickClickGame(
                instructionsResId = R.string.message_game_tutorial_combine_conditions_not_visible_target,
                scoreToReach = 30,
                durationSeconds = 10,
                rules = TwoStillTargetsPressWhenOneVisibleRules(),
            ),
            steps = listOf(
                // Beginning, hide the overlay for now
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    newVisibility = false,
                ),
                // Start screen, before first play.
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_1,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // First play lost, make floating menu visible
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.GameLost,
                    newVisibility = true,
                ),
                // First play lost, open edit scenario and go to screen condition list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_2,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.MAIN_MENU_BUTTON_CONFIG,
                    ),
                ),
                // Screen condition list: create first image condition and capture red target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_3,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCREEN_CONDITIONS_BRIEF_MENU
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_CREATE,
                    ),
                ),
                // Image condition dialog: toggle "is visible" to no
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_4,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.IMAGE_CONDITION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_DIALOG_FIELD_VISIBILITY,
                    ),
                ),
                // Screen condition dialog: save condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_5,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Screen condition list: create second image condition adn capture blue target
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_6,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.SCREEN_CONDITIONS_BRIEF_MENU
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_CREATE,
                    ),
                ),
                // Image condition dialog: Save condition and go back to event dialog
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_7,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.IMAGE_CONDITION,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Event dialog: open actions list and create new click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_8,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.EVENT,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Click Dialog: Select "click on condition"
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_9,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.CLICK,
                    ),
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_FIELD_POSITION_TYPE_ITEM_ON_CONDITION,
                    ),
                ),
                // Scenario saved, start it and start the game
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_10,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed(
                        MonitoredOverlayType.MAIN_MENU,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Game won
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_11,
                    stepStartCondition = TutorialStepStartCondition.GameWon,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
            )
        )

}