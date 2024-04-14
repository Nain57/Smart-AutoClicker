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
package com.buzbuz.smartautoclicker.core.bitmaps.di

import android.content.Context
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapManager
import com.buzbuz.smartautoclicker.core.bitmaps.IBitmapManager

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BitmapsHiltModule {

    @Provides
    @Singleton
    fun providesAppDataDir(@ApplicationContext context: Context): File =
        context.filesDir

    @Provides
    @Singleton
    fun providesBitmapManager(appDataDir: File): IBitmapManager =
        BitmapManager(appDataDir)
}