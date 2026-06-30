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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.TutorialCategory


internal fun getBasicsCategory() =
    TutorialCategory(
        type = TutorialCategory.Type.BASICS,
        nameRes = R.string.tutorial_category_basics_name,
        shortDescriptionRes = R.string.tutorial_category_basics_desc_short,
        longDescriptionRes = R.string.tutorial_category_basics_desc_long,
        iconRes = R.drawable.ic_category_basics,
        content = listOf(
            TutorialCategory.Content.Category(TutorialCategory.Type.SCREEN_CONDITIONS),
            TutorialCategory.Content.Category(TutorialCategory.Type.TRIGGER_CONDITIONS),
            TutorialCategory.Content.Category(TutorialCategory.Type.ACTIONS),
        ),
    )