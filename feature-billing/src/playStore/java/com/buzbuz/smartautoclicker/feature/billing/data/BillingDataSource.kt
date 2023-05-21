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
package com.buzbuz.smartautoclicker.feature.billing.data

import android.app.Activity
import android.content.Context
import android.util.Log

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class BillingDataSource(
    applicationContext: Context,
    private val defaultScope: CoroutineScope,
) {
    /** Google Play billing client. */
    private val billingClient = BillingClient.newBuilder(applicationContext)
        .setListener(::onPurchasesUpdated)
        .enablePendingPurchases()
        .build()

    /** Manages the connection of the [BillingClient]. */
    private val billingServiceConnection = BillingServiceConnection(
        billingClient,
        ::onBillingClientConnected,
    )
    /** Manages the [ProductDetails] for the [PRODUCT_ID]. */
    private val productDetailsManager = ProductDetailsManager(
        billingClient,
        PRODUCT_ID,
        defaultScope,
    )
    /** Manages the [Purchase] of a product. */
    private val purchaseManager = PurchaseManager(
        billingClient,
        PRODUCT_ID,
        defaultScope,
        ::onPurchaseUpdateFailed,
    )

    private val billingFlowInProcess = MutableStateFlow(false)

    init {
        billingClient.startConnection(billingServiceConnection)
    }

    fun getNewPurchases() = purchaseManager.newPurchaseFlow.asSharedFlow()

    /**
     * Returns whether or not the user has purchased a product.
     * It does this by returning a Flow that returns true if the product is in the PURCHASED state and the purchase has
     * been acknowledged.
     *
     * @return a Flow that observes the product purchase state
     */
    fun isPurchased(): Flow<Boolean> = purchaseManager.productState
        .map { state -> state == ProductState.PRODUCT_STATE_PURCHASED_AND_ACKNOWLEDGED }

    /**
     * Returns whether or not the user can purchase a product.
     * It does this by returning a Flow combine transformation that returns true if the product is in the UNSPECIFIED
     * state, as well as if we have productDetails for the product.
     * (Product cannot be purchased without valid ProductDetails.)
     *
     * @return a Flow that observes the SKUs purchase state
     */
    fun canPurchase(): Flow<Boolean> = purchaseManager.productState
        .combine(productDetailsManager.productDetails) { state, details ->
            state == ProductState.PRODUCT_STATE_NOT_PURCHASED && details != null
        }

    fun getProductTitle(): Flow<String> = productDetailsManager.productDetails
        .mapNotNull { details -> details?.title }

    fun getProductPrice(): Flow<String> = productDetailsManager.productDetails
        .mapNotNull { details -> details?.oneTimePurchaseOfferDetails?.formattedPrice }

    fun getProductDescription(): Flow<String> = productDetailsManager.productDetails
        .mapNotNull { skuDetails -> skuDetails?.description }

    /**
     * Returns a Flow that reports if a billing flow is in process, meaning that launchBillingFlow has returned
     * BillingResponseCode.OK and onPurchasesUpdated hasn't yet been called.
     *
     * @return Flow that indicates the known state of the billing flow.
     */
    fun getBillingFlowInProcess(): Flow<Boolean> = billingFlowInProcess.asStateFlow()

    /**
     * Launch the billing flow.
     *
     * @param activity active activity to launch our billing flow from.
     * @return true if launch is successful.
     */
    fun launchBillingFlow(activity: Activity) {
        val details = productDetailsManager.productDetails.value

        if (details == null) {
            Log.e(LOG_TAG, "ProductDetails not found.")
            return
        }

        val productDetailsParams = ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        defaultScope.launch {
            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                billingFlowInProcess.emit(true)
            } else {
                Log.e(LOG_TAG, "Billing failed: " + billingResult.debugMessage)
            }
        }
    }

    suspend fun refreshPurchases() {
        purchaseManager.refreshPurchases()
    }

    private fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        purchaseManager.onPurchasesUpdated(billingResult, purchases)
    }

    private fun onBillingClientConnected() {
        defaultScope.launch {
            productDetailsManager.queryProductDetailsAsync()
            refreshPurchases()
        }
    }

    private fun onPurchaseUpdateFailed() {
        defaultScope.launch {
            billingFlowInProcess.emit(false)
        }
    }
}

/** Tag for logs. */
private const val LOG_TAG = "BillingDataSource"
/** Product id as declared in the google play console. */
private const val PRODUCT_ID = "sac_pro"
