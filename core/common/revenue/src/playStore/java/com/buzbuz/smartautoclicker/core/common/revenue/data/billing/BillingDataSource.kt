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
package com.buzbuz.smartautoclicker.core.common.revenue.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.Purchase

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.common.quality.BuildConfig
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk.BillingClientProxy
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk.BillingServiceConnection
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk.BillingUiFlowState
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk.InAppProduct

import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    billingServiceConnection: BillingServiceConnection,
    private val productDetailsManager: ProductDetailsManager,
    private val purchaseManager: NewPurchaseManager,
) {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    private var billingClient: BillingClientProxy? = null
    /** Refreshes the purchases after the billing flow reported the product as already owned. */
    private var alreadyOwnedRefreshJob: Job? = null

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
        coroutineScopeIo.launch { purchaseManager.restoreCachedPurchase() }

        billingServiceConnection.connect(
            productId = PRODUCT_ID,
            onClientAvailable = ::onBillingClientAvailable,
            onNewPurchasesListener = ::onPurchaseUpdatedFromBillingUiFlow,
            onAlreadyOwnedListener = ::onAlreadyOwnedFromBillingUiFlow,
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

    private fun onBillingClientAvailable(client: BillingClientProxy) {
        billingClient = client

        coroutineScopeIo.launch {
            productDetailsManager.startMonitoring(client::fetchInAppProductDetails)
            purchaseManager.refreshPurchasesUntilSettled(client::fetchInAppPurchases, client::acknowledgePurchase)
        }
    }

    private fun onPurchaseUpdatedFromBillingUiFlow(purchase: Purchase?) {
        val client = billingClient ?: return

        coroutineScopeIo.launch {
            purchaseManager.handleUiFlowPurchase(purchase, client::acknowledgePurchase)
        }
    }

    private fun onAlreadyOwnedFromBillingUiFlow() {
        val client = billingClient ?: return

        alreadyOwnedRefreshJob?.cancel()
        alreadyOwnedRefreshJob = coroutineScopeIo.launch {
            purchaseManager.refreshPurchasesUntilFound(client::fetchInAppPurchases, client::acknowledgePurchase)
        }
    }

    fun refreshPurchases(): Job? {
        val client = billingClient ?: return null

        return coroutineScopeIo.launch {
            purchaseManager.handlePurchasesQueryResult(client.fetchInAppPurchases(), client::acknowledgePurchase)
        }
    }

    /**
     * Refreshes the purchases if Google Play never answered the purchases query in this process, and waits for the
     * result. Cancelling the wait doesn't cancel the refresh.
     */
    suspend fun refreshUnsettledPurchases() {
        if (purchaseManager.isStateSettled.value) return
        refreshPurchases()?.join()
    }

    fun launchBillingFlow(activity: Activity): StateFlow<BillingUiFlowState>? {
        val client = billingClient ?: return null
        val details = productDetailsManager.productDetails.value ?: return null

        return client.launchBillingFlow(activity, details)
    }
}

/** Product id as declared in the google play console. */
private const val PRODUCT_ID = "sac_pro"
