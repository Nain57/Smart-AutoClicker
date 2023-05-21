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

import android.app.Activity
import android.content.Context

import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.feature.billing.data.BillingDataSource
import com.buzbuz.smartautoclicker.feature.billing.ui.ProModeBillingActivity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class BillingRepository(applicationContext: Context): IBillingRepository {

    /** The scope for the flows declared for the billing. */
    private val billingScope = CoroutineScope(Job() + Dispatchers.IO)
    /** Manages the connexion with the billing API and provide status about pro mode product state. */
    private val dataSource: BillingDataSource = BillingDataSource(applicationContext, billingScope)
    /** Tells if the [ProModeBillingActivity] is started or not. */
    private val billingActivityState = MutableStateFlow(false)

    override val newPurchases: Flow<List<String>> = dataSource.getNewPurchases()

    override val isProModePurchased: Flow<Boolean> = dataSource.isPurchased()
    override val canPurchaseProMode = dataSource.canPurchase()

    override val proModeTitle: Flow<String> = dataSource.getProductTitle()
    override val proModePrice: Flow<String> = dataSource.getProductPrice()
    override val proModeDescription: Flow<String> = dataSource.getProductDescription()

    override val isBillingFlowInProcess: Flow<Boolean> = billingActivityState

    override fun startBillingActivity(context: Context, requestedAdvantage: ProModeAdvantage) {
        context.startActivity(ProModeBillingActivity.getStartIntent(context, requestedAdvantage))
    }

    internal fun launchPlayStoreBillingFlow(activity: Activity) {
        dataSource.launchBillingFlow(activity)
    }

    internal fun setBillingActivityState(created: Boolean) {
        billingActivityState.value = created
    }
}