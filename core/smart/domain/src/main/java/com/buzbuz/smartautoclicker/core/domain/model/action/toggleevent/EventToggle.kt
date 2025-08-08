
package com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent

data class EventToggle(
    override val id: Identifier,
    val actionId: Identifier,
    val targetEventId: Identifier?,
    val toggleType: ToggleEvent.ToggleType,
): Identifiable, Completable {

    override fun isComplete(): Boolean = true
}