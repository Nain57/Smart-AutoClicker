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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialSlideshow

internal fun getActionsListSlideshow() =
    TutorialSlideshow(
        type = TutorialSlideshow.Type.ACTIONS_LIST,
        nameRes = R.string.tutorial_slideshow_actions_list_title,
        shortDescriptionRes = R.string.tutorial_slideshow_actions_list_desc,
        slideshowItems = listOf(
            TutorialSlideshow.SlideshowItem(
                tutorialTextRes = R.string.tutorial_slideshow_actions_list_step_1_text,
                tutorialImage = R.drawable.tutorial_instructions_action_order,
                tutorialImageFormat = TutorialSlideshow.ImageFormat.IMAGE_LARGE,
            ),
        ),
    )
