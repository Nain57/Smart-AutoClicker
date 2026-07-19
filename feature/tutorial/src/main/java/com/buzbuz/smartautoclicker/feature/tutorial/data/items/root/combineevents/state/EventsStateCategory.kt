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
package com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineevents.state

import com.buzbuz.smartautoclicker.feature.tutorial.R
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategory
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialItem
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialSlideshow


internal fun getEventsStateCategory() =
    TutorialCategory(
        type = TutorialCategory.Type.EVENTS_STATE,
        nameRes = R.string.tutorial_category_events_state_name,
        shortDescriptionRes = R.string.tutorial_category_events_state_desc_short,
        longDescriptionRes = R.string.tutorial_category_events_state_desc_long,
        iconRes = R.drawable.ic_toggle_event,
        content = listOf(
            TutorialCategory.Content.Divider,
            TutorialCategory.Content.Slideshow(TutorialSlideshow.Type.EVENTS_STATE_BASICS),
            TutorialCategory.Content.Tutorial(TutorialItem.Type.EVENTS_STATE),
        ),
    )