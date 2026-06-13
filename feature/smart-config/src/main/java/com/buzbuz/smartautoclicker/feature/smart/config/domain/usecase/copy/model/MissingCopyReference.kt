/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/** Indicates an item reference that is not present in the results of a copy.*/
sealed class MissingCopyReference {

    abstract val name: String

    /** An Event referenced by an EventToggle from a ToggleEvent can't be found in the current scenario. */
    data class EventToggleReference(
        override val name: String,
    ): MissingCopyReference()

    /** A ScreenCondition referenced by a click can't be found in the currently edited event. */
    data class ScreenConditionReference(
        override val name: String,
        val conditionId: Identifier,
    ): MissingCopyReference()

    /** A Counter referenced by an item can't be found in the current scenario. */
    data class CounterReference(
        override val name: String,
    ) : MissingCopyReference()
}