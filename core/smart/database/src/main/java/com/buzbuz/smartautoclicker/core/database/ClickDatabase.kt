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
)
abstract class ClickDatabase : ScenarioDatabase()

/** Current version of the database. */
const val CLICK_DATABASE_VERSION = 1