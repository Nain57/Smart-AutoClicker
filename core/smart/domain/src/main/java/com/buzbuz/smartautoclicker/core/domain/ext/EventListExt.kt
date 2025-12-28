/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.ext

import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.core.domain.model.action.Notification
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.Event


fun List<Event>.getAllCounterNames(): Set<String> = buildSet {
    this@getAllCounterNames.forEach { event ->
        event.conditions.forEach { condition ->
            if (condition is TriggerCondition.OnCounterCountReached) add(condition.counterName)
        }

        event.actions.forEach { action ->
            if (action is ChangeCounter) add(action.counterName)
            if (action is Notification && action.messageType == Notification.MessageType.COUNTER_VALUE)
                add(action.messageCounterName)
        }
    }
}