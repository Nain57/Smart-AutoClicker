/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.domain.edition

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.EndCondition
import com.buzbuz.smartautoclicker.domain.Event
import com.buzbuz.smartautoclicker.domain.Scenario

/** Represents the scenario currently edited. */
data class EditedScenario internal constructor(
    val scenario: Scenario,
    val endConditions: List<EditedEndCondition>,
    val events: List<EditedEvent>,
)

/** Represents the end conditions of the scenario currently edited. */
data class EditedEndCondition internal constructor(
    val endCondition: EndCondition,
    val itemId: Int,
    val eventItemId: Int = INVALID_EDITED_ITEM_ID,
)

/** Represents the events of the scenario currently edited. */
data class EditedEvent internal constructor(
    val event: Event,
    val itemId: Int,
    val editedActions: List<EditedAction>,
)

/** Represents the actions of the event currently edited. */
data class EditedAction internal constructor(
    val action: Action,
    val toggleEventItemId: Int = INVALID_EDITED_ITEM_ID,
)

/** Invalid edited item id. */
const val INVALID_EDITED_ITEM_ID = -1