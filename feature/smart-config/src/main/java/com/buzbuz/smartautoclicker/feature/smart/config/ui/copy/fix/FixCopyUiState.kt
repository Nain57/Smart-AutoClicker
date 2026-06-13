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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action.UiAction
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiEvent

data class FixEventsCopyUiState(
    val canBeCopied: Boolean,
    val items: List<FixCopyUiItem>,
)

data class FixEventsChildrenCopyUiState(
    val canBeCopied: Boolean,
    val items: List<FixCopyUiItem>,
)

sealed class FixCopyUiItem {

    data class Header(
        @field:StringRes val message: Int,
    ): FixCopyUiItem()

    sealed class Item : FixCopyUiItem() {

        abstract val isValidForCopy: Boolean

        data class EventItem(
            val uiEvent: UiEvent,
            override val isValidForCopy: Boolean,
        ) : Item()

        sealed class EventChildren : Item() {

            abstract val stateText: String
            abstract val itemWithMissingReferences: ItemWithMissingReferences

            data class ActionItem(
                val uiAction: UiAction,
                override val stateText: String,
                override val isValidForCopy: Boolean,
                override val itemWithMissingReferences: ItemWithMissingReferences.ActionItem,
            ) : EventChildren()

            data class ConditionItem(
                val uiCondition: UiCondition,
                override val stateText: String,
                override val isValidForCopy: Boolean,
                override val itemWithMissingReferences: ItemWithMissingReferences.ConditionItem,
            ) : EventChildren()
        }
    }
}