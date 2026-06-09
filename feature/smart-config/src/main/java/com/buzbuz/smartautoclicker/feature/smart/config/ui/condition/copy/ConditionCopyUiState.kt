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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.copy

import androidx.annotation.StringRes
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.condition.UiTriggerCondition

data class ConditionCopyUiState(
    val items: List<ConditionCopyItem>,
    val thisEventSize: Int,
    val thisScenarioSize: Int,
    val otherScenarioSize: Int,
)

/** Types of items in the condition copy list. */
sealed class ConditionCopyItem {

    /**
     * Header item, delimiting sections.
     * @param title the title for the header.
     */
    data class HeaderItem(@field:StringRes val title: Int) : ConditionCopyItem()

    sealed class ConditionItem : ConditionCopyItem() {

        abstract val uiCondition: UiCondition
        abstract val isChecked: Boolean


        /**
         * Screen Condition item.
         * @param uiCondition the details for the condition.
         */
        data class Screen(
            override val uiCondition: UiScreenCondition,
            override val isChecked: Boolean,
        ) : ConditionItem()

        /**
         * Trigger Condition item.
         * @param uiCondition the details for the condition.
         */
        data class Trigger(
            override val uiCondition: UiTriggerCondition,
            override val isChecked: Boolean,
        ) : ConditionItem()
    }
}