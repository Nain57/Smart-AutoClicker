
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