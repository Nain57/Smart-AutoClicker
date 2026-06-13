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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.event

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiTriggerEvent

/** Types of items in the event copy list. */
sealed class EventCopyItem {

    /**
     * Header item, delimiting sections.
     * @param title the title for the header.
     */
    data class Header(
        @field:StringRes val title: Int,
    ) : EventCopyItem()

    sealed class EventItem : EventCopyItem() {

        abstract val name: String
        abstract val uiEvent: UiEvent
        abstract val checked: Boolean

        data class Image (
            override val name: String,
            override val uiEvent: UiImageEvent,
            override val checked: Boolean,
            val actionsIcons: List<Int>,
        ) : EventItem()

        data class Trigger (
            override val name: String,
            override val uiEvent: UiTriggerEvent,
            override val checked: Boolean,
        ) : EventItem()
    }

}