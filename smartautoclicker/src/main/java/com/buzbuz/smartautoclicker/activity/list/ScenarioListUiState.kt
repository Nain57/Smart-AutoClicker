/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.activity.list

import androidx.annotation.IntRange

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_DISABLED_ITEM_INT
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.ALPHA_ENABLED_ITEM_INT

/**
 * Ui State for the [ScenarioListFragment]
 *
 * @param type the current ui type
 * @param menuUiState the ui state for the action bar menu
 * @param listContent the content of the scenario list
 * @param isProModePurchased tells if the user have bought pro mode
 */
data class ScenarioListUiState(
    val type: Type,
    val menuUiState: Menu,
    val listContent: List<Item>,
    val isProModePurchased: Boolean,
) {

    /** Possible states for the action menu of the ScenarioListFragment. */
    enum class Type {
        /** The user can select a scenario to be played/edited.*/
        SELECTION,
        /** The user is searching for a scenario. */
        SEARCH,
        /** The user is selecting the scenarios to export. */
        EXPORT,
    }

    /**
     * Ui state of the action menu.
     *
     * @param searchItemState the state of the search item.
     * @param selectAllItemState the state of the select all item.
     * @param cancelItemState the state of the cancel item.
     * @param importItemState the state of the import item.
     * @param exportItemState the state of the export item.
     */
    sealed class Menu(
        val searchItemState: Item,
        val selectAllItemState: Item,
        val cancelItemState: Item,
        val importItemState: Item,
        val exportItemState: Item,
    ) {

        /**
         * Defines a menu item in the action bar.
         *
         * @param visible true if it should be visible, false if not.
         * @param enabled true if the user can interact with it, false if not.
         * @param iconAlpha the alpha to apply to the icon.
         */
        data class Item(
            val visible: Boolean,
            val enabled: Boolean = true,
            @IntRange(from = 0, to = 255) val iconAlpha: Int = 255,
        )

        data object Search : Menu(
            searchItemState = Item(false),
            selectAllItemState = Item(false),
            cancelItemState = Item(false),
            importItemState = Item(false),
            exportItemState = Item(false),
        )

        data class Export(private val canExport: Boolean) : Menu(
            searchItemState = Item(false),
            selectAllItemState = Item(true),
            cancelItemState = Item(true),
            importItemState = Item(false),
            exportItemState = Item(
                visible = true,
                enabled = canExport,
                iconAlpha = if (canExport) ALPHA_ENABLED_ITEM_INT else ALPHA_DISABLED_ITEM_INT,
            ),
        )

        data class Selection(
            private val searchEnabled: Boolean,
            private val exportEnabled: Boolean,
            private val isProMode: Boolean,
        ) : Menu(
            searchItemState = Item(searchEnabled),
            selectAllItemState = Item(false),
            cancelItemState = Item(false),
            importItemState = Item(
                visible = true,
                enabled = true,
                iconAlpha = if (isProMode) ALPHA_ENABLED_ITEM_INT else ALPHA_DISABLED_ITEM_INT,
            ),
            exportItemState = Item(
                visible = exportEnabled,
                enabled = exportEnabled,
                iconAlpha = if (isProMode) ALPHA_ENABLED_ITEM_INT else ALPHA_DISABLED_ITEM_INT,
            ),
        )
    }

    sealed class Item(val displayName: String, val scenarioTypeIcon: Int) {

        abstract val scenario: Any

        sealed class Empty(displayName: String, scenarioTypeIcon: Int) : Item(displayName, scenarioTypeIcon) {
            data class Dumb(
                override val scenario: DumbScenario,
            ) : Empty(displayName = scenario.name, scenarioTypeIcon = R.drawable.ic_dumb)

            data class Smart(
                override val scenario: Scenario,
            ) : Empty(displayName = scenario.name, scenarioTypeIcon = R.drawable.ic_smart)
        }

        sealed class Valid(displayName: String, scenarioTypeIcon: Int) : Item(displayName, scenarioTypeIcon) {

            abstract val showExportCheckbox: Boolean
            abstract val checkedForExport: Boolean
            data class Dumb(
                override val scenario: DumbScenario,
                override val showExportCheckbox: Boolean = false,
                override val checkedForExport: Boolean = false,
                val clickCount: Int,
                val swipeCount: Int,
                val pauseCount: Int,
                val repeatText: String,
                val maxDurationText: String,
            ) : Valid(displayName = scenario.name,  scenarioTypeIcon = R.drawable.ic_dumb)

            data class Smart(
                override val scenario: Scenario,
                override val showExportCheckbox: Boolean = false,
                override val checkedForExport: Boolean = false,
                val eventsItems: List<EventItem>,
            ) : Valid(displayName = scenario.name, scenarioTypeIcon = R.drawable.ic_smart) {

                data class EventItem(
                    val id: Long,
                    val eventName: String,
                    val actionsCount: Int,
                    val conditionsCount: Int,
                    val firstCondition: Condition?,
                )
            }
        }
    }
}