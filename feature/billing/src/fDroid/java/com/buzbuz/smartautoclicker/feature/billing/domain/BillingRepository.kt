/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.billing.domain

import android.content.Context

import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf


@Suppress("UNUSED_PARAMETER") // Required by other build variants
class BillingRepository(applicationContext: Context): IBillingRepository {

    override val newPurchases: Flow<List<String>> = flowOf(emptyList())

    override val isProModePurchased: Flow<Boolean> = flowOf(true)
    override val canPurchaseProMode: Flow<Boolean> = flowOf(false)

    override val proModeTitle: Flow<String> = flowOf("")
    override val proModePrice: Flow<String> = flowOf("")
    override val proModeDescription: Flow<String> = flowOf("")

    override val isBillingFlowInProcess: Flow<Boolean> = flowOf(false)
    override fun startBillingActivity(context: Context, requestedAdvantage: ProModeAdvantage) {}
}