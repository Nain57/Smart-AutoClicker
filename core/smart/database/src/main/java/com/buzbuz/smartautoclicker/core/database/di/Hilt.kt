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
package com.buzbuz.smartautoclicker.core.database.di

import android.content.Context
import androidx.room.Room

import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.TutorialDatabase
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

    @Provides
    @Singleton
    fun providesTutorialDatabase(
        @ApplicationContext context: Context,
    ): TutorialDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            TutorialDatabase::class.java,
            "tutorial_database",
        ).addMigrations(
            Migration10to11,
            Migration12to13,
        ).build()
}