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
package com.buzbuz.smartautoclicker.scenarios.list.model

import androidx.annotation.IntRange

import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortType

/**
 * Ui State for the [com.buzbuz.smartautoclicker.scenarios.list.ScenarioListFragment]
 *
 * @param type the current ui type
 * @param menuUiState the ui state for the action bar menu
 * @param listContent the content of the scenario list
 */
data class ScenarioListUiState(
    val type: Type,
    val menuUiState: Menu,
    val listContent: List<Item>,
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

    /** Ui state of the action menu. */
    sealed class Menu(
        val searchItemState: Item = Item(false),
        val selectAllItemState: Item = Item(false),
        val cancelItemState: Item = Item(false),
        val importExportItemState: Item = Item(false),
        val tutorialsItemState: Item = Item(false),
        val settingsItemState: Item = Item(false),
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
            @field:IntRange(from = 0, to = 255) val iconAlpha: Int = 255,
        )

        data object Search : Menu()

        data class Export(private val canExport: Boolean) : Menu(
            selectAllItemState = Item(true),
            cancelItemState = Item(true),
            importExportItemState = Item(
                visible = true,
                enabled = canExport,
            )
        )

        data class Selection(
            private val searchEnabled: Boolean,
        ) : Menu(
            searchItemState = Item(searchEnabled),
            selectAllItemState = Item(false),
            cancelItemState = Item(false),
            importExportItemState = Item(
                visible = true,
                enabled = true,
            ),
            tutorialsItemState = Item(
                visible = true,
                enabled = true,
            ),
            settingsItemState = Item(true),
        )
    }

    sealed class Item {

        data class SortItem(
            val sortType: ScenarioSortType,
            val smartVisible: Boolean,
            val dumbVisible: Boolean,
            val changeOrderChecked: Boolean,
        ): Item()

        sealed class ScenarioItem(val displayName: String): Item() {

            abstract val scenario: Any
            abstract val lastStartTimestamp: Long
            abstract val startCount: Long

            sealed class Empty(displayName: String) : ScenarioItem(displayName) {
                data class Dumb(
                    override val scenario: DumbScenario,
                    override val lastStartTimestamp: Long,
                    override val startCount: Long,
                ) : Empty(displayName = scenario.name)

                data class Smart(
                    override val scenario: Scenario,
                    override val lastStartTimestamp: Long,
                    override val startCount: Long,
                ) : Empty(displayName = scenario.name)
            }

            sealed class Valid(displayName: String) : ScenarioItem(displayName) {

                abstract val showExportCheckbox: Boolean
                abstract val checkedForExport: Boolean
                abstract val expanded: Boolean

                abstract fun getScenarioId(): Long

                data class Dumb(
                    override val scenario: DumbScenario,
                    override val showExportCheckbox: Boolean = false,
                    override val checkedForExport: Boolean = false,
                    override val expanded: Boolean = false,
                    override val lastStartTimestamp: Long,
                    override val startCount: Long,
                    val clickCount: Int,
                    val swipeCount: Int,
                    val pauseCount: Int,
                    val repeatText: String,
                    val maxDurationText: String,
                ) : Valid(displayName = scenario.name) {
                    override fun getScenarioId(): Long = scenario.id.databaseId
                }

                data class Smart(
                    override val scenario: Scenario,
                    override val showExportCheckbox: Boolean = false,
                    override val checkedForExport: Boolean = false,
                    override val expanded: Boolean = false,
                    override val lastStartTimestamp: Long,
                    override val startCount: Long,
                    val eventsItems: List<EventItem>,
                    val triggerEventCount: Int,
                    val detectionQuality: Int,
                ) : Valid(displayName = scenario.name) {

                    override fun getScenarioId(): Long = scenario.id.databaseId

                    data class EventItem(
                        val id: Long,
                        val eventName: String,
                        val actionsCount: Int,
                        val conditionsCount: Int,
                        val firstCondition: ScreenCondition?,
                    )
                }
            }
        }
    }
}