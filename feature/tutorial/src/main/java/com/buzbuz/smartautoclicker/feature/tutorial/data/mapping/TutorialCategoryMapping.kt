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
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.changecounter.getChangeCounterActionCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.changeeventstate.getChangeEventStateActionCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.click.getClickActionCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.intent.getIntentActionCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.notification.getNotificationActionCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.pause.getPauseActionCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.swipe.getSwipeActionCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.system.getSystemActionCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.writetext.getWriteTextActionCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.getBasicsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.color.getColorConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.getScreenConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.getImageConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.number.getNumberConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.text.getTextConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.triggerconditions.getTriggerConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineconditions.getCombineConditionsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.counters.getCountersCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineevents.getCombineEventsCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineevents.priority.getEventsPriorityCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.combineevents.state.getEventsStateCategory
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.getRootCategory


internal fun TutorialCategory.Type.toTutorialCategory(): TutorialCategory =
    when (this) {
        ACTIONS -> getActionsCategory()
        BASICS -> getBasicsCategory()
        CHANGE_COUNTER_ACTION -> getChangeCounterActionCategory()
        TOGGLE_EVENT_ACTION -> getChangeEventStateActionCategory()
        CLICK_ACTION -> getClickActionCategory()
        COLOR_CONDITION -> getColorConditionsCategory()
        COMBINE_CONDITIONS -> getCombineConditionsCategory()
        COUNTERS -> getCountersCategory()
        COMBINE_EVENTS -> getCombineEventsCategory()
        EVENTS_PRIORITY -> getEventsPriorityCategory()
        EVENTS_STATE -> getEventsStateCategory()
        IMAGE_CONDITION -> getImageConditionsCategory()
        INTENT_ACTION -> getIntentActionCategory()
        NOTIFICATION_ACTION -> getNotificationActionCategory()
        NUMBER_CONDITION -> getNumberConditionsCategory()
        PAUSE_ACTION -> getPauseActionCategory()
        ROOT -> getRootCategory()
        SCREEN_CONDITIONS -> getScreenConditionsCategory()
        SWIPE_ACTION -> getSwipeActionCategory()
        SYSTEM_ACTION -> getSystemActionCategory()
        TEXT_CONDITION -> getTextConditionsCategory()
        TRIGGER_CONDITIONS -> getTriggerConditionsCategory()
        WRITE_TEXT_ACTION -> getWriteTextActionCategory()
    }
