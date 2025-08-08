
package com.buzbuz.smartautoclicker.core.database.di

import android.content.Context
import androidx.room.Room

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.migrations.Migration10to11
import com.buzbuz.smartautoclicker.core.database.migrations.Migration12to13
import com.buzbuz.smartautoclicker.core.database.migrations.Migration1to2
import com.buzbuz.smartautoclicker.core.database.migrations.Migration2to3
import com.buzbuz.smartautoclicker.core.database.migrations.Migration3to4
import com.buzbuz.smartautoclicker.core.database.migrations.Migration4to5
import com.buzbuz.smartautoclicker.core.database.migrations.Migration5to6
import com.buzbuz.smartautoclicker.core.database.migrations.Migration6to7
import com.buzbuz.smartautoclicker.core.database.migrations.Migration9to10

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SmartDatabaseModule {
    @Provides
    @Singleton
    fun providesClickDatabase(
        @ApplicationContext context: Context,
    ): ClickDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            ClickDatabase::class.java,
            "click_database"
        ).addMigrations(
            Migration1to2,
            Migration2to3,
            Migration3to4,
            Migration4to5,
            Migration5to6,
            Migration6to7,
            Migration9to10,
            Migration10to11,
            Migration12to13,
        ).build()
}