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
package com.buzbuz.smartautoclicker.feature.revenue.domain

import android.app.Activity

import com.buzbuz.smartautoclicker.feature.revenue.domain.model.AdState
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.ProModeInfo
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.PurchaseState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow


internal interface InternalRevenueRepository {

    val proModeInfo: Flow<ProModeInfo?>
    val adsState: StateFlow<AdState>
    val purchaseState: StateFlow<PurchaseState>

    fun showAd(activity: Activity)
    fun requestTrial()

    fun startPlayStoreBillingUiFlow(activity: Activity)

    fun setBillingActivityDestroyed()
}