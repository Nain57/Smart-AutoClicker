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
package com.buzbuz.smartautoclicker.core.dumb.di

import android.content.Context
import androidx.room.Room

import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.IDumbRepository
import dagger.Binds

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DumbDatabaseModule {
    @Provides
    @Singleton
    fun providesDumbDatabase(
        @ApplicationContext context: Context,
    ): DumbDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            DumbDatabase::class.java,
            "dumb_database"
        ).build()
}

@Module
@InstallIn(SingletonComponent::class)
interface DumbModule {

    @Binds
    @Singleton
    fun providesDumbRepository(dumbRepository: DumbRepository): IDumbRepository
}