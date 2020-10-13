/*
 * Copyright (C) 2020 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database

import com.buzbuz.smartautoclicker.database.room.ScenarioEntity
import com.buzbuz.smartautoclicker.database.room.ScenarioWithClicks

/**
 * Object defining a click scenario.
 *
 * This is basically a holder of the data of scenario contained in database. It handles to conversion between the simple
 * types that a database can contains into more complex types easier to use in the application code.
 *
 * @param name the name of the click.
 * @param id the unique identifier for the scenario. Use 0 to let the database create the id. Default value is 0.
 * @param clickCount the number of clicks in this scenario. Default value is 0.
 */
data class ClickScenario(
    val name: String,
    val id: Long = 0,
    val clickCount: Int = 0
) {

    companion object {

        /**
         * Convert a list of [ScenarioWithClicks] into a list of [ClickScenario].
         *
         * @param entities the scenario to be converted.
         *
         * @return the list of corresponding scenario.
         */
        internal fun fromEntities(entities: List<ScenarioWithClicks>?) : List<ClickScenario> {
            return entities?.map { entity ->
                ClickScenario(
                    entity.scenario.name,
                    entity.scenario.id,
                    entity.clicks.count()
                )
            } ?: emptyList()
        }
    }

    /**
     * Convert this scenario info into a [ScenarioEntity] ready to be inserted into the database.
     *
     * @return the scenario, ready to be inserted.
     */
    internal fun toEntity() : ScenarioEntity =
        ScenarioEntity(id, name)
}