
package com.buzbuz.smartautoclicker.core.database.di

import android.content.Context
import androidx.room.Room

import com.buzbuz.smartautoclicker.core.database.ClickDatabase

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
        ).build()
}