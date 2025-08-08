
package com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleEntity
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent

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
        toggleType = ToggleEvent.ToggleType.valueOf(type.name),
    )