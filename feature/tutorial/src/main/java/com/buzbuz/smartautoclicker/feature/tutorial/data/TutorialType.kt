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
package com.buzbuz.smartautoclicker.feature.tutorial.data

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.Tutorial
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.getOneMovingTargetTutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.getOneStillTargetTutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.getTwoMovingTargetsPressInOrderTutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.getTwoStillTargetsPressWhenBothVisibleTutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.getTwoStillTargetsPressWhenOneVisibleTutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.newOneMovingTargetTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.newOneStillTargetTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.newTwoMovingTargetsPressInOrderTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.newTwoStillTargetsPressWhenBothVisibleTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.newTwoStillTargetsPressWhenOneVisibleTutorial

internal enum class TutorialType {
    IMAGE_DETECTION_MOVING_TARGET,
    IMAGE_DETECTION_STILL_TARGET,
    IMAGE_DETECTION_TWO_MOVING_TARGETS_PRESS_IN_ORDER,
    IMAGE_DETECTION_TWO_STILL_TARGETS_PRESS_WHEN_BOTH,
    IMAGE_DETECTION_TWO_STILL_TARGETS_PRESS_WHEN_ONE,
}

internal fun TutorialType.getTutorialInfo() : TutorialInfo =
    when (this) {
        TutorialType.IMAGE_DETECTION_MOVING_TARGET -> getOneMovingTargetTutorialInfo()
        TutorialType.IMAGE_DETECTION_STILL_TARGET -> getOneStillTargetTutorialInfo()
        TutorialType.IMAGE_DETECTION_TWO_MOVING_TARGETS_PRESS_IN_ORDER -> getTwoMovingTargetsPressInOrderTutorialInfo()
        TutorialType.IMAGE_DETECTION_TWO_STILL_TARGETS_PRESS_WHEN_BOTH -> getTwoStillTargetsPressWhenBothVisibleTutorialInfo()
        TutorialType.IMAGE_DETECTION_TWO_STILL_TARGETS_PRESS_WHEN_ONE -> getTwoStillTargetsPressWhenOneVisibleTutorialInfo()
    }

internal fun TutorialType.getTutorial(): Tutorial =
    when (this) {
        TutorialType.IMAGE_DETECTION_MOVING_TARGET -> newOneMovingTargetTutorial()
        TutorialType.IMAGE_DETECTION_STILL_TARGET -> newOneStillTargetTutorial()
        TutorialType.IMAGE_DETECTION_TWO_MOVING_TARGETS_PRESS_IN_ORDER -> newTwoMovingTargetsPressInOrderTutorial()
        TutorialType.IMAGE_DETECTION_TWO_STILL_TARGETS_PRESS_WHEN_BOTH -> newTwoStillTargetsPressWhenBothVisibleTutorial()
        TutorialType.IMAGE_DETECTION_TWO_STILL_TARGETS_PRESS_WHEN_ONE -> newTwoStillTargetsPressWhenOneVisibleTutorial()
    }
