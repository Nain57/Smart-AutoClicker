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

import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.getActionsListSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.changecounter.getChangeCounterActionSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.click.getClickActionOffsetSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.click.getClickActionTargetSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.pause.getPauseActionSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.swipe.getSwipeActionSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.intent.getIntentActionSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.notification.getNotificationActionSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.changeeventstate.getChangeEventStateActionSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.system.getSystemActionSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.actions.writetext.getWriteTextActionSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.getScreenConditionsThresholdSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.getScreenConditionsTypeSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.getImageConditionsCaptureSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.getImageConditionsDetectionAreaSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.number.getNumberConditionsDetectionAreaSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.text.getTextConditionsDetectionAreaSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.triggerconditions.getBroadcastReceivedSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.triggerconditions.getCounterReachedSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialSlideshow


internal fun TutorialSlideshow.Type.toTutorialSlideshow(): TutorialSlideshow =
    when (this) {
        TutorialSlideshow.Type.ACTIONS_LIST -> getActionsListSlideshow()
        TutorialSlideshow.Type.BROADCAST_RECEIVED_CONDITION -> getBroadcastReceivedSlideshow()
        TutorialSlideshow.Type.CHANGE_COUNTER_ACTION -> getChangeCounterActionSlideshow()
        TutorialSlideshow.Type.CLICK_ACTION_OFFSET -> getClickActionOffsetSlideshow()
        TutorialSlideshow.Type.CLICK_ACTION_TARGET -> getClickActionTargetSlideshow()
        TutorialSlideshow.Type.COUNTER_REACHED_CONDITION -> getCounterReachedSlideshow()
        TutorialSlideshow.Type.IMAGE_CONDITION_CAPTURE -> getImageConditionsCaptureSlideshow()
        TutorialSlideshow.Type.IMAGE_CONDITION_DETECTION_AREA -> getImageConditionsDetectionAreaSlideshow()
        TutorialSlideshow.Type.INTENT_ACTION -> getIntentActionSlideshow()
        TutorialSlideshow.Type.NOTIFICATION_ACTION -> getNotificationActionSlideshow()
        TutorialSlideshow.Type.NUMBER_CONDITION_DETECTION_AREA -> getNumberConditionsDetectionAreaSlideshow()
        TutorialSlideshow.Type.PAUSE_ACTION -> getPauseActionSlideshow()
        TutorialSlideshow.Type.SCREEN_CONDITIONS_DETECTION_THRESHOLD -> getScreenConditionsThresholdSlideshow()
        TutorialSlideshow.Type.SCREEN_CONDITIONS_TYPE -> getScreenConditionsTypeSlideshow()
        TutorialSlideshow.Type.SWIPE_ACTION -> getSwipeActionSlideshow()
        TutorialSlideshow.Type.SYSTEM_ACTION -> getSystemActionSlideshow()
        TutorialSlideshow.Type.TOGGLE_EVENT_ACTION -> getChangeEventStateActionSlideshow()
        TutorialSlideshow.Type.TEXT_CONDITION_DETECTION_AREA -> getTextConditionsDetectionAreaSlideshow()
        TutorialSlideshow.Type.WRITE_TEXT_ACTION -> getWriteTextActionSlideshow()
    }
