/*
 * Copyright (C) 2025 Kevin Buzeau
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
import android.content.Context
import com.android.billingclient.api.Purchase

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.feature.revenue.BuildConfig
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk.InAppProduct
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk.BillingClientProxy
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk.BillingServiceConnection
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk.BillingUiFlowState
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BillingDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(IO) ioDispatcher: CoroutineDispatcher,
    private val billingServiceConnection: BillingServiceConnection,
    private val productDetailsManager: ProductDetailsManager,
    private val purchaseManager: NewPurchaseManager,
) {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    private var billingClient: BillingClientProxy? = null

    private val debugProduct: MutableStateFlow<InAppProduct?> =
        MutableStateFlow(null)
    val product: Flow<InAppProduct?> = combine(productDetailsManager.productDetails, debugProduct) { details, debugDetails ->
        debugDetails ?: details
    }

    private val debugPurchaseState: MutableStateFlow<InAppPurchaseState?> =
        MutableStateFlow(null)
    val purchaseState: Flow<InAppPurchaseState> = combine(purchaseManager.state, debugPurchaseState) { state, debugState ->
        debugState ?: state
    }

    private var debugReceiver: DebugBillingStateReceiver? = null

    init {
        billingServiceConnection.monitorConnection(
            productId = PRODUCT_ID,
            onConnectionChangedListener = { client ->
                if (client != null) onBillingClientConnected(client)
                else onBillingClientDisconnected()
            },
            onNewPurchasesListener = ::onPurchaseUpdatedFromBillingUiFlow
        )

        if (BuildConfig.DEBUG) {
            debugReceiver = DebugBillingStateReceiver { billingState ->
                debugPurchaseState.value = billingState
                debugProduct.value =
                    if (billingState == null) null
                    else InAppProduct.Debug()

            }.apply { register(context) }
        }
    }

    private fun onBillingClientConnected(client: BillingClientProxy) {
        billingClient = client

        coroutineScopeIo.launch {
            productDetailsManager.startMonitoring(client::fetchInAppProductDetails)
            refreshPurchases()
        }
    }

    private fun onBillingClientDisconnected() {
        billingClient = null
        productDetailsManager.stopMonitoring()
    }

    private fun onPurchaseUpdatedFromBillingUiFlow(purchase: Purchase?) {
        val client = billingClient ?: return

        coroutineScopeIo.launch {
            client.refreshPurchases(purchase, fromQuery = false)
        }
    }

    fun refreshPurchases() {
        val client = billingClient ?: return

        coroutineScopeIo.launch {
            client.refreshPurchases(
                purchase = client.fetchInAppPurchases(),
                fromQuery = true,
            )
        }
    }

    fun launchBillingFlow(activity: Activity): StateFlow<BillingUiFlowState>? {
        val client = billingClient ?: return null
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
