
package com.buzbuz.smartautoclicker.core.domain.model.action

import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.Completable
import com.buzbuz.smartautoclicker.core.base.interfaces.Prioritizable

/** Base for for all possible actions for an Event. */
sealed class Action : Identifiable, Completable, Prioritizable {

    /** The identifier of the event for this action. */
    abstract val eventId: Identifier
    /** The name of the action. */
    abstract val name: String?

    /** @return true if this action is complete and can be transformed into its entity. */
    override fun isComplete(): Boolean = name != null

    abstract fun hashCodeNoIds(): Int

    /** @return creates a deep copy of this action. */
    abstract fun deepCopy(): Action

    fun copyBase(
        id: Identifier = this.id,
        eventId: Identifier = this.eventId,
        name: String? = this.name,
        priority: Int = this.priority,
    ): Action =
        when (this) {
            is Click -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ChangeCounter -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Intent -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Pause -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Swipe -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is ToggleEvent -> copy(id = id, eventId = eventId, name = name, priority = priority)
            is Notification -> copy(id = id, eventId = eventId, name = name, priority = priority)
        }
}
