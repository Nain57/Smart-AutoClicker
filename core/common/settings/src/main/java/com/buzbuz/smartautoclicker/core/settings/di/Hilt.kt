
package com.buzbuz.smartautoclicker.core.settings.di

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.settings.data.SettingsDataSource
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository
import com.buzbuz.smartautoclicker.core.settings.SettingsRepositoryImpl

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
    ): SettingsRepository = SettingsRepositoryImpl(ioDispatcher, dataSource)
}