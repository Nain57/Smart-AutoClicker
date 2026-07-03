package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineconditions

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepImage
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.subjects.game.image.TwoStillTargetsPressWhenOneVisibleRules
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
            subject = TutorialSubject.Game(
                instructionsResId = R.string.message_game_tutorial_combine_conditions_not_visible_target,
                scoreToReach = 30,
                durationSeconds = 10,
                rules = TwoStillTargetsPressWhenOneVisibleRules(),
            ),
            // TODO: we need to guard the not visible with another visible condition, or a click spam will block the tutorial
            steps = listOf(
                /*
                // Beginning, hide the overlay for now
                TutorialStep.ChangeFloatingUiVisibility(
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    newVisibility = false,
                ),
                // Start screen, before first play
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
                // First play lost, open edit scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_3,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.MAIN_MENU_BUTTON_CONFIG,
                    ),
                ),
                // Create Event
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_4,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_CREATE_EVENT,
                    ),
                ),
                // Select condition field
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_5,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_CONDITIONS,
                    ),
                ),
                // Create a new condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_6,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_CREATE,
                    ),
                ),
                // Create a new condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_7,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_TYPE_SELECTION_IMAGE,
                    ),
                ),
                // Take a screenshot
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_8,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_capture,
                        imageDescResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_secondary_8,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Ensure target is captured or retry
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_9,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_cancel,
                        imageDescResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_secondary_9,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_CAPTURE_MENU_BUTTON_CAPTURE,
                    ),
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Talk about area
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_10,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Change visibility
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_11,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_DIALOG_FIELD_VISIBILITY,
                    ),
                ),
                // Save condition
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_12,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCREEN_CONDITION_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close condition list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_13,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CONDITIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Select action field
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_14,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_FIELD_ACTIONS,
                    ),
                ),
                // Create a new action
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_15,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_CREATE_ACTION,
                    ),
                ),
                // Create a new click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_16,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTION_TYPE_DIALOG_CLICK_ACTION,
                    ),
                ),
                // Select click location
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_17,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_FIELD_SELECT_POSITION_OR_CONDITION,
                    ),
                ),
                // Pick location
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_18,
                    image = TutorialStepImage(
                        imageResId = R.drawable.ic_visible_on,
                        imageDescResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_secondary_18,
                    ),
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                // Save click
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_19,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Close action list
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_20,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.ACTIONS_BRIEF_MENU_BUTTON_SAVE,
                    ),
                ),
                // Save Event
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_21,
                    stepStartCondition = TutorialStepStartCondition.Immediate,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.EVENT_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Save scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_22,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.SCENARIO_DIALOG_BUTTON_SAVE,
                    ),
                ),
                // Play scenario
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_23,
                    stepStartCondition = TutorialStepStartCondition.MonitoredOverlayDisplayed,
                    stepEndCondition = TutorialStepEndCondition.MonitoredViewClicked(
                        MonitoredViewType.MAIN_MENU_BUTTON_PLAY,
                    ),
                ),
                // Game won
                TutorialStep.TutorialOverlay(
                    contentTextResId = R.string.message_tutorial_combine_conditions_not_visible_target_step_24,
                    stepStartCondition = TutorialStepStartCondition.GameWon,
                    stepEndCondition = TutorialStepEndCondition.NextButton,
                ),
                */
            )
        )

}