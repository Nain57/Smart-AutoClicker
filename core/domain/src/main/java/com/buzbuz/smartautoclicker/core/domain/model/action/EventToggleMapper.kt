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
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleEntity

internal fun EventToggle.toEntity(): EventToggleEntity =
    EventToggleEntity(
        id = id.databaseId,
        actionId = actionId.databaseId,
        toggleEventId = targetEventId!!.databaseId,
        type = toggleType.toEntity(),
    )

internal fun EventToggleEntity.toDomain(cleanIds: Boolean = false): EventToggle =
    EventToggle(
        id = Identifier(id, cleanIds),
        actionId = Identifier(actionId, cleanIds),
        targetEventId = Identifier(toggleEventId, cleanIds),
        toggleType = Action.ToggleEvent.ToggleType.valueOf(type.name),
    )