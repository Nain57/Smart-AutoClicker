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
package com.buzbuz.smartautoclicker.feature.tutorial.di

import com.buzbuz.smartautoclicker.core.common.navigation.TutorialNavigator
import com.buzbuz.smartautoclicker.core.common.overlays.di.OverlayComponent
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialsProvider
import com.buzbuz.smartautoclicker.feature.tutorial.data.TutorialsProviderImpl
import com.buzbuz.smartautoclicker.feature.tutorial.navigation.TutorialNavigatorImpl
import com.buzbuz.smartautoclicker.feature.tutorial.ui.overlay.TutorialOverlayViewModel

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@EntryPoint
@InstallIn(OverlayComponent::class)
interface TutorialViewModelsEntryPoint {
    fun tutorialOverlayViewModel(): TutorialOverlayViewModel
}

@Module
@InstallIn(SingletonComponent::class)
object TutorialFeatureModule {

    @Provides
    @Singleton
    internal fun providesTutorialsProvider(provider: TutorialsProviderImpl): TutorialsProvider =
        provider
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TutorialNavigationModule {

    @Binds
    @Singleton
    abstract fun bindTutorialNavigator(impl: TutorialNavigatorImpl): TutorialNavigator
}