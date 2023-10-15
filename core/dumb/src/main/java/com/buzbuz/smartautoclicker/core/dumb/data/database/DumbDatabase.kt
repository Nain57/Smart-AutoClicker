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
package com.buzbuz.smartautoclicker.core.dumb.data.database

import android.content.Context

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        DumbScenarioEntity::class,
        DumbActionEntity::class,
    ],
    version = DUMB_DATABASE_VERSION,
    exportSchema = true,
)
@TypeConverters(
    DumbActionTypeStringConverter::class,
)
abstract class DumbDatabase : RoomDatabase() {

    companion object {

        /** Singleton preventing multiple instances of database opening at the same time. */
        @Volatile
        private var INSTANCE: DumbDatabase? = null

        /**
         * Get the Room database singleton, or instantiates it if it wasn't yet.
         * <p>
         * @param context the Android context.
         * <p>
         * @return the Room database singleton.
         */
        fun getDatabase(context: Context): DumbDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DumbDatabase::class.java,
                    "dumb_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }

    /** The data access object for the dumb scenario in the database. */
    abstract fun dumbScenarioDao(): DumbScenarioDao
}

/** Current version of the database. */
const val DUMB_DATABASE_VERSION = 1