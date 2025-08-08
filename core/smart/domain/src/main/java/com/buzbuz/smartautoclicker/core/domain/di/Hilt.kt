
package com.buzbuz.smartautoclicker.core.domain.di

import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.core.domain.data.ScenarioDataSource

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryHiltModule {

    @Provides
    @Singleton
    internal fun providesRepository(
        dataSource: ScenarioDataSource,
        bitmapManager: BitmapRepository,
    ): IRepository = Repository(dataSource, bitmapManager)
}