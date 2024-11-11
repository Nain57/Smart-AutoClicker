/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.domain.model

import android.content.Intent
import com.buzbuz.smartautoclicker.core.base.AndroidExecutor

/** Execute the actions related to Android. */
interface SmartActionExecutor : AndroidExecutor {

    /** Start the activity defined by the provided intent. */
    fun executeStartActivity(intent: Intent)

    /** Send a broadcast defined by the provided intent. */
    fun executeSendBroadcast(intent: Intent)

    /** Send a notification defined by the provided NotificationRequest. */
    fun executeNotification(notification: NotificationRequest)

    /** Request to reset any state related to action execution, most likely because a new session is starting. */
    fun clearState()
}

data class NotificationRequest(
    val actionId: Long,
    val eventId: Long,
    val title: String,
    val message: String,
    val groupName: String,
    val importance: Int,
)