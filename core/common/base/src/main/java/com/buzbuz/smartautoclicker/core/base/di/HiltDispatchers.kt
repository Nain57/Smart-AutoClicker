
package com.buzbuz.smartautoclicker.core.base.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Dispatcher(HiltCoroutineDispatchers.IO)
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(HiltCoroutineDispatchers.Main)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Main
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: HiltCoroutineDispatchers)

enum class HiltCoroutineDispatchers {
    Main,
    IO,
}