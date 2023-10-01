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

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.TypeConverters
import com.buzbuz.smartautoclicker.core.database.dao.TutorialDao

import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ActionTypeStringConverter
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionTypeStringConverter
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EndConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraTypeStringConverter
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.database.entity.ToggleEventTypeStringConverter
import com.buzbuz.smartautoclicker.core.database.entity.TutorialSuccessEntity
import com.buzbuz.smartautoclicker.core.database.migrations.AutoMigration8to9
import com.buzbuz.smartautoclicker.core.database.migrations.Migration10to11
import com.buzbuz.smartautoclicker.core.database.migrations.Migration1to2

@Database(
    entities = [
        ActionEntity::class,
        EventEntity::class,
        ScenarioEntity::class,
        ConditionEntity::class,
        EndConditionEntity::class,
        IntentExtraEntity::class,
        TutorialSuccessEntity::class,
    ],
    version = TUTORIAL_DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 11, to = 12),
    ]
)
@TypeConverters(
    ActionTypeStringConverter::class,
    ClickPositionTypeStringConverter::class,
    IntentExtraTypeStringConverter::class,
    ToggleEventTypeStringConverter::class,
)
abstract class TutorialDatabase : ScenarioDatabase() {

    abstract fun tutorialDao(): TutorialDao

    companion object {

        /** Singleton preventing multiple instances of database opening at the same time. */
        @Volatile
        private var INSTANCE: TutorialDatabase? = null

        /**
         * Get the Room database singleton, or instantiates it if it wasn't yet.
         * <p>
         * @param context the Android context.
         * <p>
         * @return the Room database singleton.
         */
        fun getDatabase(context: Context): TutorialDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TutorialDatabase::class.java,
                    "tutorial_database",
                )
                    .addMigrations(
                        Migration10to11,
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

/** Current version of the database. */
const val TUTORIAL_DATABASE_VERSION = CLICK_DATABASE_VERSION