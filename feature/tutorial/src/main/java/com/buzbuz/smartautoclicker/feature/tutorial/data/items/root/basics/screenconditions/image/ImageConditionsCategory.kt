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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.TutorialCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.TutorialItem

internal fun getImageConditionsCategory() =
    TutorialCategory(
        type = TutorialCategory.Type.IMAGE_CONDITION,
        nameRes = R.string.tutorial_category_image_condition_name,
        shortDescriptionRes = R.string.tutorial_category_image_condition_desc_short,
        longDescriptionRes = R.string.tutorial_category_image_condition_desc_long,
        iconRes = R.drawable.ic_condition,
        content = listOf(
            TutorialCategory.Content.Tutorial(TutorialItem.Type.IMAGE_DETECTION_STILL_TARGET),
            TutorialCategory.Content.Tutorial(TutorialItem.Type.IMAGE_DETECTION_MOVING_TARGET),
            TutorialCategory.Content.Tutorial(TutorialItem.Type.IMAGE_DETECTION_TWO_STILL_TARGETS_PRESS_WHEN_BOTH),
        ),
    )