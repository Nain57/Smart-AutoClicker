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
package com.buzbuz.smartautoclicker.core.database

import androidx.room.RoomDatabase
import com.buzbuz.smartautoclicker.core.database.dao.ActionDao
import com.buzbuz.smartautoclicker.core.database.dao.ConditionDao
import com.buzbuz.smartautoclicker.core.database.dao.EndConditionDao
import com.buzbuz.smartautoclicker.core.database.dao.EventDao
import com.buzbuz.smartautoclicker.core.database.dao.ScenarioDao

abstract class ScenarioDatabase : RoomDatabase() {

    /** The data access object for the scenario in the database. */
    abstract fun scenarioDao(): ScenarioDao
    /** The data access object for the events in the database. */
    abstract fun eventDao(): EventDao
    /** The data access object for the conditions in the database. */
    abstract fun conditionDao(): ConditionDao
    /** The data access object for the actions in the database. */
    abstract fun actionDao(): ActionDao
    /** The data access object for the end conditions in the database. */
    abstract fun endConditionDao(): EndConditionDao

}