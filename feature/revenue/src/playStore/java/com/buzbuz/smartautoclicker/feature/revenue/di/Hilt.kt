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
package com.buzbuz.smartautoclicker.feature.revenue.di

import com.buzbuz.smartautoclicker.feature.revenue.data.ads.sdk.IAdsSdk
import com.buzbuz.smartautoclicker.feature.revenue.data.ads.sdk.GoogleAdsSdk
import com.buzbuz.smartautoclicker.feature.revenue.domain.RevenueRepository
import com.buzbuz.smartautoclicker.feature.revenue.domain.InternalRevenueRepository

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PlayStoreBillingHiltModule {

    @Provides
    @Singleton
    internal fun providesAdsSdk(googleAdsSdk: GoogleAdsSdk): IAdsSdk =
        googleAdsSdk

    @Provides
    @Singleton
    internal fun providesInternalRevenueRepository(repository: RevenueRepository): InternalRevenueRepository =
        repository
}