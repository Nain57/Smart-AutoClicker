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

import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategory
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategory.Type.*
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.getActionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.getBasicsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.color.getColorConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.getScreenConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.getImageConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.number.getNumberConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.text.getTextConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.triggerconditions.getTriggerConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineconditions.getCombineConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.counters.getCountersCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.eventstate.getEventStateCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.getRootCategory


internal fun TutorialCategory.Type.toTutorialCategory(): TutorialCategory =
    when (this) {
        ACTIONS -> getActionsCategory()
        BASICS -> getBasicsCategory()
        COLOR_CONDITION -> getColorConditionsCategory()
        COMBINE_CONDITIONS -> getCombineConditionsCategory()
        COUNTERS -> getCountersCategory()
        EVENT_STATE -> getEventStateCategory()
        IMAGE_CONDITION -> getImageConditionsCategory()
        NUMBER_CONDITION -> getNumberConditionsCategory()
        ROOT -> getRootCategory()
        SCREEN_CONDITIONS -> getScreenConditionsCategory()
        TEXT_CONDITION -> getTextConditionsCategory()
        TRIGGER_CONDITIONS -> getTriggerConditionsCategory()
    }
