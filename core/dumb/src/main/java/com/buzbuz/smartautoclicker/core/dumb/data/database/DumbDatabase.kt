
package com.buzbuz.smartautoclicker.core.dumb.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

import javax.inject.Singleton


@Database(
    entities = [
        DumbScenarioEntity::class,
        DumbActionEntity::class,
        DumbScenarioStatsEntity::class,
    ],
    version = DUMB_DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2),
    ]
)
@TypeConverters(
    DumbActionTypeStringConverter::class,
)
@Singleton
abstract class DumbDatabase : RoomDatabase() {

    /** The data access object for the dumb scenario in the database. */
    abstract fun dumbScenarioDao(): DumbScenarioDao
}

/** Current version of the database. */
const val DUMB_DATABASE_VERSION = 2