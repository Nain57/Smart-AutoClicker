package com.buzbuz.smartautoclicker.core.database

import androidx.room.RoomDatabase
import com.buzbuz.smartautoclicker.core.database.dao.ActionDao
import com.buzbuz.smartautoclicker.core.database.dao.ConditionDao
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

}