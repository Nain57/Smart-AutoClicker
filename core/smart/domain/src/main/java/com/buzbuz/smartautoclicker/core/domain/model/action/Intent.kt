
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.content.ComponentName
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra

/**
 * Intent action.
 *
 * @param id the unique identifier for the action.
 * @param eventId the identifier of the event for this action.
 * @param name the name of the action.
 * @param isAdvanced if false, the user have used the simple config. If true, the advanced config.
 * @param isBroadcast true if this intent should be a broadcast, false for a startActivity.
 * @param intentAction the action of the intent.
 * @param componentName the component name for the intent. Can be null for a broadcast.
 * @param flags the flags for the intent.
 * @param extras the list of extras to sent with the intent.
 */
data class Intent(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String? = null,
    override var priority: Int,
    val isAdvanced: Boolean? = null,
    val isBroadcast: Boolean,
    val intentAction: String? = null,
    val componentName: ComponentName? = null,
    val flags: Int? = null,
    val extras: List<IntentExtra<out Any>>? = null,
) : Action() {

    override fun isComplete(): Boolean {
        if (!super.isComplete()) return false

        if (isAdvanced == null || intentAction == null || flags == null) return false
        extras?.forEach { extra -> if (!extra.isComplete()) return false }

        return true
    }

    override fun hashCodeNoIds(): Int =
        name.hashCode() + isAdvanced.hashCode() + isBroadcast.hashCode() + intentAction.hashCode() +
                componentName.hashCode() + flags.hashCode() + extras.hashCode()

    override fun deepCopy(): Intent = copy(name = "" + name)
}