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
package com.buzbuz.smartautoclicker.core.domain.model.action

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.NotificationMessageType

data class Notification(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val messageType: MessageType,
    val messageText: String,
    val messageCounterName: String,
    val channelImportance: Int,
) : Action() {

    /**
     * Types of messages of a [Notification].
     * Keep the same names as the db ones.
     */
    enum class MessageType {
        /** Display the text defined by [Notification.messageText]. */
        TEXT,
        /** Display the counter value of the counter [Notification.messageCounterName]. */
        COUNTER_VALUE;

        fun toEntity(): NotificationMessageType = NotificationMessageType.valueOf(name)
    }

    override fun hashCodeNoIds(): Int =
        name.hashCode() + channelImportance.hashCode() + messageType.hashCode() +
                messageText.hashCode() + messageCounterName.hashCode()

    override fun deepCopy(): Notification = copy(
        name = "" + name,
        messageText = "" + messageText,
        messageCounterName = "" + messageCounterName,
    )

    override fun isComplete(): Boolean =
        super.isComplete() && isMessageValid()

    private fun isMessageValid(): Boolean =
        (messageType == MessageType.TEXT && messageText.isNotEmpty())
                || (messageType == MessageType.COUNTER_VALUE && messageCounterName.isNotEmpty())
}