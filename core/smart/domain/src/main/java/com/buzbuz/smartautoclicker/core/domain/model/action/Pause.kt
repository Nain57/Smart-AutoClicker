
package com.buzbuz.smartautoclicker.core.domain.model.action

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/**
 * Pause action.
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param pauseDuration the duration of the pause in milliseconds.
 */
data class Pause(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val pauseDuration: Long? = null,
) : Action() {

    override fun isComplete(): Boolean = super.isComplete() && pauseDuration != null

    override fun hashCodeNoIds(): Int =
        name.hashCode() + pauseDuration.hashCode()


    override fun deepCopy(): Pause = copy(name = "" + name)
}