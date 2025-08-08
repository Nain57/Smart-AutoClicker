
package com.buzbuz.smartautoclicker.core.bitmaps.di

import android.content.Context
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapLRUCache
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepositoryImpl
import com.buzbuz.smartautoclicker.core.bitmaps.ConditionBitmapsDataSource

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
    internal fun providesBitmapRepository(
        bitmapLRUCache: BitmapLRUCache,
        conditionBitmapsDataSource: ConditionBitmapsDataSource,
    ): BitmapRepository = BitmapRepositoryImpl(bitmapLRUCache, conditionBitmapsDataSource)
}