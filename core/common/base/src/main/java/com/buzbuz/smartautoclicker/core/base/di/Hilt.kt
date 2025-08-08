
package com.buzbuz.smartautoclicker.core.base.di

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BaseHiltModule {

    @Provides
    @Singleton
    fun providesAppComponentsProvider(appComponentsManager: AppComponentsManager): AppComponentsProvider =
        appComponentsManager
}