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
package com.buzbuz.smartautoclicker.core.smart.debugging.di

import com.buzbuz.smartautoclicker.core.processing.domain.ProcessingListener
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepositoryImpl
import com.buzbuz.smartautoclicker.core.smart.debugging.engine.DebugEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object SmartDebuggingModule {

    @Provides
    @Singleton
    internal fun providesDebuggingRepository(debuggingRepositoryImpl: DebuggingRepositoryImpl): DebuggingRepository =
        debuggingRepositoryImpl

    @Provides
    @Singleton
    internal fun providesDebuggingListener(debugEngine: DebugEngine): ProcessingListener =
        debugEngine
}
