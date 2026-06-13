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

import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition


/** An Item (Action or Condition) that references another item that is not present in the results of a copy. */
sealed class ItemWithMissingReferences {

    /** The item referencing something missing. */
    abstract val item: Identifiable
    /** The missing referenced items. */
    abstract val missingReferences: List<MissingCopyReference>

    data class ActionItem(
        override val item: Action,
        override val missingReferences: List<MissingCopyReference>
    ) : ItemWithMissingReferences()

    data class ConditionItem(
        override val item: Condition,
        override val missingReferences: List<MissingCopyReference>
    ) : ItemWithMissingReferences()
}