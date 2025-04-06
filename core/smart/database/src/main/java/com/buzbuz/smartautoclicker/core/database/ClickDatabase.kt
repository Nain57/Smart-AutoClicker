/*
 * Copyright (C) 2025 Kevin Buzeau
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

import androidx.room.AutoMigration
import androidx.room.Database

import com.buzbuz.smartautoclicker.core.database.entity.ActionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventEntity
import com.buzbuz.smartautoclicker.core.database.entity.EventToggleEntity
import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioStatsEntity
import com.buzbuz.smartautoclicker.core.database.migrations.*

import javax.inject.Singleton

@Singleton
@Database(
    entities = [
        ActionEntity::class,
        EventEntity::class,
        ScenarioEntity::class,
        ConditionEntity::class,
        IntentExtraEntity::class,
        EventToggleEntity::class,
        ScenarioStatsEntity::class,
    ],
    version = CLICK_DATABASE_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 7, to = 8),
        AutoMigration (from = 8, to = 9, spec = Migration8to9::class),
        AutoMigration (from = 11, to = 12),
        AutoMigration (from = 13, to = 14),
        AutoMigration (from = 14, to = 15),
        AutoMigration (from = 15, to = 16),
        AutoMigration (from = 16, to = 17),
    ]
)
abstract class ClickDatabase : ScenarioDatabase()

/** Current version of the database. */
const val CLICK_DATABASE_VERSION = 17