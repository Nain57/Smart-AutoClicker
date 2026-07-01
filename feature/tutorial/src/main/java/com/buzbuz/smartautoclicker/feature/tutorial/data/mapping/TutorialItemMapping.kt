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
package com.buzbuz.smartautoclicker.feature.tutorial.data.mapping

import com.buzbuz.smartautoclicker.feature.tutorial.data.items.TutorialItem
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.TutorialItem.Type.*
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.ImageConditionsMovingTargetTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.ImageConditionsStillTargetTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.ImageConditionsNotVisibleTargetTutorial


internal fun TutorialItem.Type.toTutorialItem(): TutorialItem =
    when (this) {
        IMAGE_DETECTION_MOVING_TARGET -> ImageConditionsMovingTargetTutorial
        IMAGE_DETECTION_STILL_TARGET -> ImageConditionsStillTargetTutorial
        IMAGE_DETECTION_TWO_STILL_TARGETS_PRESS_WHEN_BOTH -> ImageConditionsNotVisibleTargetTutorial
    }
