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