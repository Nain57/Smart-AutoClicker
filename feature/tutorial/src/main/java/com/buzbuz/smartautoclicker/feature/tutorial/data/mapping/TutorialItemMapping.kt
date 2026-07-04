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

import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.color.ColorConditionsTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem.Type.*
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.ImageConditionsMovingTargetTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.ImageConditionsStillTargetTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.text.TextConditionsMovingTextTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.text.TextConditionsStillTextTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineconditions.CombineConditionsNotVisibleTargetTutorial


internal fun TutorialItem.Type.toTutorialItem(): TutorialItem =
    when (this) {
        COLOR_CONDITION -> ColorConditionsTutorial

        COMBINE_CONDITIONS_NOT_VISIBLE -> CombineConditionsNotVisibleTargetTutorial

        IMAGE_DETECTION_MOVING_TARGET -> ImageConditionsMovingTargetTutorial
        IMAGE_DETECTION_STILL_TARGET -> ImageConditionsStillTargetTutorial

        TEXT_CONDITION_MOVING_TEXT -> TextConditionsMovingTextTutorial
        TEXT_CONDITION_STILL_TEXT -> TextConditionsStillTextTutorial
    }
