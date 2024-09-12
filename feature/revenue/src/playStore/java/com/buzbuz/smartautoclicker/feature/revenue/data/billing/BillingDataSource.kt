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
package com.buzbuz.smartautoclicker.feature.revenue.data.billing

import android.app.Activity
import com.android.billingclient.api.Purchase

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk.InAppProduct
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk.BillingClientProxy
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk.BillingServiceConnection
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk.BillingUiFlowState

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BillingDataSource @Inject constructor(
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val billingServiceConnection: BillingServiceConnection,
    private val productDetailsManager: ProductDetailsManager,
    private val purchaseManager: NewPurchaseManager,
) {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)


    val product: Flow<InAppProduct?> =
        productDetailsManager.productDetails

    val purchaseState: Flow<InAppPurchaseState> =
        purchaseManager.state


    init {
        billingServiceConnection.monitorConnection(
            productId = PRODUCT_ID,
            onConnectionChangedListener = { client ->
                if (client != null) onBillingClientConnected(client)
                else onBillingClientDisconnected()
            },
            onNewPurchasesListener = ::onPurchaseUpdatedFromBillingUiFlow
        )
    }

    private fun onBillingClientConnected(client: BillingClientProxy) {
        coroutineScopeIo.launch {
            productDetailsManager.startMonitoring(client::fetchInAppProductDetails)
            refreshPurchases()
        }
    }

    private fun onBillingClientDisconnected() {
        productDetailsManager.stopMonitoring()
    }

    private fun onPurchaseUpdatedFromBillingUiFlow(purchase: Purchase?) {
        coroutineScopeIo.launch {
            billingServiceConnection.clientProxy?.refreshPurchases(purchase, fromQuery = false)
        }
    }

    fun refreshPurchases() {
        val client = billingServiceConnection.clientProxy ?: return

        coroutineScopeIo.launch {
            client.refreshPurchases(
                purchase = client.fetchInAppPurchases(),
                fromQuery = true,
            )
        }
    }

    fun launchBillingFlow(activity: Activity): StateFlow<BillingUiFlowState>? {
        val client = billingServiceConnection.clientProxy ?: return null
        val details = productDetailsManager.productDetails.value ?: return null

        return client.launchBillingFlow(activity, details)
    }


    private suspend fun BillingClientProxy.refreshPurchases(purchase: Purchase?, fromQuery: Boolean) {
        purchaseManager.handleNewPurchases(
            purchase = purchase,
            fromFetch = fromQuery,
            ackQuery = ::acknowledgePurchase,
       )
    }
}

/** Product id as declared in the google play console. */
private const val PRODUCT_ID = "sac_pro"
