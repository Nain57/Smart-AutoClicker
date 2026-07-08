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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialSlideshow

internal fun getScreenConditionsTypeSlideshow() =
    TutorialSlideshow(
        type = TutorialSlideshow.Type.SCREEN_CONDITIONS_TYPE,
        nameRes = R.string.tutorial_slideshow_screen_condition_type_title,
        shortDescriptionRes = R.string.tutorial_slideshow_screen_condition_type_desc,
        slideshowItems = listOf(
            TutorialSlideshow.SlideshowItem(
                tutorialTextRes = R.string.tutorial_slideshow_screen_condition_type_step_1_text,
                tutorialImage = R.drawable.ic_color_condition,
                tutorialImageFormat = TutorialSlideshow.ImageFormat.ICON,
            ),
            TutorialSlideshow.SlideshowItem(
                tutorialTextRes = R.string.tutorial_slideshow_screen_condition_type_step_2_text,
                tutorialImage = R.drawable.ic_image_condition,
                tutorialImageFormat = TutorialSlideshow.ImageFormat.ICON,
            ),
            TutorialSlideshow.SlideshowItem(
                tutorialTextRes = R.string.tutorial_slideshow_screen_condition_type_step_3_text,
                tutorialImage = R.drawable.ic_text_condition,
                tutorialImageFormat = TutorialSlideshow.ImageFormat.ICON,
            ),
        ),
    )