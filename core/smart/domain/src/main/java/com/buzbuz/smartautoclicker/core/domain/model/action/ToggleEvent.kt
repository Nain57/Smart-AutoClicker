
package com.buzbuz.smartautoclicker.core.domain.model.action

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.base.interfaces.areComplete
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleType
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle

/**
 * Toggle Event Action.
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param toggleAll true to toggle all events, false to control only via EventToggle.
 * @param toggleAllType the type of manipulation to apply for toggle all.
 */
data class ToggleEvent(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val toggleAll: Boolean = false,
    val toggleAllType: ToggleType? = null,
    val eventToggles: List<EventToggle> = emptyList(),
) : Action() {

    /**
     * Types of toggle of a [ToggleEvent].
     * Keep the same names as the db ones.
     */
    enum class ToggleType {
        /** Enable the event. Has no effect if the event is already enabled. */
        ENABLE,
        /** Disable the event. Has no effect if the event is already disabled. */
        DISABLE,
        /** Enable the event if it is disabled, disable it if it is enabled. */
        TOGGLE;

        fun toEntity(): EventToggleType = EventToggleType.valueOf(name)
    }

    override fun isComplete(): Boolean {
        if (!super.isComplete()) return false

        return if (toggleAll) {
            toggleAllType != null
        } else {
            eventToggles.areComplete()
        }
    }

    override fun hashCodeNoIds(): Int =
        name.hashCode() + toggleAll.hashCode() + toggleAllType.hashCode() + eventToggles.hashCode()

    override fun deepCopy(): ToggleEvent = copy(name = "" + name)
}