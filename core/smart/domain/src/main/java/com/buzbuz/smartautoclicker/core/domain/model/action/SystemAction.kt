/*
 * Copyright (C) 2025 Kevin Buzeau
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
import com.buzbuz.smartautoclicker.core.database.entity.SystemActionType

data class SystemAction(
    override val id: Identifier,
    override val eventId: Identifier,
    override val name: String?,
    override var priority: Int,
    val type: Type,
) : Action() {


    /**
     * Types of toggle of a [SystemAction].
     * Keep the same names as the db ones.
     */
    enum class Type {
        /** Execute the Android back action. */
        BACK,
        /** Return to home launcher. */
        HOME,
        /** Open recent apps screen. */
        RECENT_APPS;

        fun toEntity(): SystemActionType = SystemActionType.valueOf(name)
    }

    override fun hashCodeNoIds(): Int =
        name.hashCode() + type.hashCode()

    override fun deepCopy(): SystemAction = copy(name = "" + name)
}