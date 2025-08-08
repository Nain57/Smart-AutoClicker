
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.graphics.Point
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/**
 * Swipe action.
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param swipeDuration the duration between the swipe start and end in milliseconds.
 * @param from the x position of the swipe start.
 * @param to the x position of the swipe end.
 */
data class Swipe(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val swipeDuration: Long? = null,
    val from: Point? = null,
    val to: Point? = null,
) : Action() {

    override fun isComplete(): Boolean =
        super.isComplete() && swipeDuration != null && from != null&& to != null

    override fun hashCodeNoIds(): Int =
        name.hashCode() + swipeDuration.hashCode() + from.hashCode() + to.hashCode()

    override fun deepCopy(): Swipe = copy(name = "" + name)
}