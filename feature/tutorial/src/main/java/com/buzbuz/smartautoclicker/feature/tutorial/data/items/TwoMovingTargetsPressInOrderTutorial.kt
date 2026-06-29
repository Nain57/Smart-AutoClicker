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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepStartCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject
import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialType
import com.buzbuz.smartautoclicker.feature.tutorial.data.subject.game.TwoMovingTargetsPressInOrderRules

internal fun getTwoMovingTargetsPressInOrderTutorialInfo(): TutorialInfo =
    TutorialInfo(
        id = TutorialType.IMAGE_DETECTION_TWO_MOVING_TARGETS_PRESS_IN_ORDER.name,
        nameResId = R.string.item_title_tutorial_5,
        descResId = R.string.item_desc_tutorial_5,
    )

internal fun newTwoMovingTargetsPressInOrderTutorial(): Tutorial =
    Tutorial(
        info = getTwoMovingTargetsPressInOrderTutorialInfo(),
        subject = TutorialSubject.Game(
            instructionsResId = R.string.message_tutorial_1_step_1,
            scoreToReach = 10,
            durationSeconds = 10,
            rules = TwoMovingTargetsPressInOrderRules(),
        ),
        steps = listOf(
            TutorialStep.TutorialOverlay(
                contentTextResId = R.string.message_tutorial_1_step_1,
                stepStartCondition = TutorialStepStartCondition.Immediate,
                stepEndCondition = TutorialStepEndCondition.NextButton,
            ),
        )
    )
