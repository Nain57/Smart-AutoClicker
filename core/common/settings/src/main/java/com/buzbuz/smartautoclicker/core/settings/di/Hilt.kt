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
package com.buzbuz.smartautoclicker.core.settings.di

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.settings.engine.data.ScenarioSortSettingsDataSource
import com.buzbuz.smartautoclicker.core.settings.engine.data.SettingsDataSource
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.core.settings.engine.SettingsRepositoryImpl

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsHiltModule {

    @Provides
    @Singleton
    internal fun providesSettingsRepository(
        @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
        dataSource: SettingsDataSource,
        scenarioSortSettingsDatasource: ScenarioSortSettingsDataSource,
    ): SettingsRepository = SettingsRepositoryImpl(ioDispatcher, dataSource, scenarioSortSettingsDatasource)
}