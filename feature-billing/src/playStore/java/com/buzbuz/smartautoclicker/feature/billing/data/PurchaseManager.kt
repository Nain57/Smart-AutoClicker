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

import android.util.Log

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class PurchaseManager(
    private val billingClient: BillingClient,
    private val productId: String,
    private val defaultScope: CoroutineScope,
    private val onPurchaseUpdateFailed: () -> Unit,
) {

    /** Purchase state of the product. */
    val productState = MutableStateFlow(ProductState.PRODUCT_STATE_NOT_PURCHASED)

    val newPurchaseFlow = MutableSharedFlow<List<String>>(extraBufferCapacity = 1)

    /** GPBLv3 now queries purchases synchronously, simplifying this flow. This only gets active purchases. */
    suspend fun refreshPurchases() {
        Log.d(LOG_TAG, "Refreshing purchases.")

        val purchasesResult = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val billingResult = purchasesResult.billingResult

        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(LOG_TAG, "Problem getting purchases: " + billingResult.debugMessage)
        } else {
            processPurchaseList(purchasesResult.purchasesList, true)
        }

        Log.d(LOG_TAG, "Refreshing purchases finished.")
    }

    fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    processPurchaseList(purchases, false)
                    return
                } else Log.d(LOG_TAG, "Null Purchase List Returned from OK response!")
            }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.i(LOG_TAG, "onPurchasesUpdated: User canceled the purchase")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                Log.i(LOG_TAG, "onPurchasesUpdated: The user already owns this item")
            BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                Log.e(LOG_TAG, "onPurchasesUpdated: Google Play \"does not recognize the configuration.")
            else ->
                Log.d(LOG_TAG, "BillingResult [" + billingResult.responseCode + "]: " + billingResult.debugMessage)
        }

        onPurchaseUpdateFailed()
    }

    /**
     * Goes through each purchase and makes sure that the purchase state is processed and the state
     * is available through Flows. Verifies signature and acknowledges purchases. PURCHASED isn't
     * returned until the purchase is acknowledged.
     *
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     *
     * Developers can choose to acknowledge purchases from a server using the
     * Google Play Developer API. The server has direct access to the user database,
     * so using the Google Play Developer API for acknowledgement might be more reliable.
     *
     * If the purchase token is not acknowledged within 3 days,
     * then Google Play will automatically refund and revoke the purchase.
     * This behavior helps ensure that users are not charged unless the user has successfully
     * received access to the content.
     * This eliminates a category of issues where users complain to developers
     * that they paid for something that the app is not giving to them.
     *
     * If a skusToUpdate list is passed-into this method, any purchases not in the list of
     * purchases will have their state set to UNPURCHASED.
     */
    private fun processPurchaseList(purchases: List<Purchase>, fromRefresh: Boolean) {
        val purchase = purchases.findProductPurchase(productId)

        // Clear purchase state of anything that didn't come with this purchase list if this is part of a refresh.
        if (fromRefresh && purchase == null) {
            Log.e(LOG_TAG, "Product purchase can't be found")
            productState.tryEmit(ProductState.PRODUCT_STATE_NOT_PURCHASED)
            return
        }

        if (purchase == null) return

        val purchaseState = purchase.purchaseState
        if (purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!isSignatureValid(purchase)) {
                Log.e(LOG_TAG, "Invalid signature. Check to make sure your public key is correct.")
                return
            }

            // Only set the purchased state after we've validated the signature.
            setSkuStateFromPurchase(purchase)
            defaultScope.launch {
                if (!purchase.isAcknowledged) {
                    // Acknowledge everything --- new purchase is not yet acknowledged
                    val billingResult = billingClient.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    )

                    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.e(LOG_TAG, "Error acknowledging purchase")
                    } else {
                        productState.tryEmit(ProductState.PRODUCT_STATE_PURCHASED_AND_ACKNOWLEDGED)
                    }
                    newPurchaseFlow.tryEmit(purchase.skus)
                }
            }
        } else {
            // Make sure the state is set
            setSkuStateFromPurchase(purchase)
        }
    }

    /**
     * Calling this means that we have the most up-to-date information for a Sku in a purchase
     * object. This uses the purchase state (Pending, Unspecified, Purchased) along with the
     * acknowledged state.
     *
     * @param purchase an up-to-date object to set the state for the Sku
     */
    private fun setSkuStateFromPurchase(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> productState.tryEmit(ProductState.PRODUCT_STATE_PENDING)
            Purchase.PurchaseState.UNSPECIFIED_STATE -> productState.tryEmit(ProductState.PRODUCT_STATE_NOT_PURCHASED)
            Purchase.PurchaseState.PURCHASED -> productState.tryEmit(
                if (purchase.isAcknowledged) ProductState.PRODUCT_STATE_PURCHASED_AND_ACKNOWLEDGED
                else ProductState.PRODUCT_STATE_PURCHASED
            )
            else -> Log.e(LOG_TAG, "Purchase in unknown state: " + purchase.purchaseState)
        }
    }

    /**
     * Ideally your implementation will comprise a secure server, rendering this check
     * unnecessary. @see [Security]
     */
    private fun isSignatureValid(purchase: Purchase): Boolean {
        return Security.verifyPurchase(purchase.originalJson, purchase.signature)
    }
}

internal enum class ProductState {
    PRODUCT_STATE_NOT_PURCHASED,
    PRODUCT_STATE_PENDING,
    PRODUCT_STATE_PURCHASED,
    PRODUCT_STATE_PURCHASED_AND_ACKNOWLEDGED,
}

private fun List<Purchase>.findProductPurchase(productId: String): Purchase? =
    if (isNullOrEmpty()) null
    else firstOrNull { purchase -> purchase.products.contains(productId) }

private const val LOG_TAG = "PurchaseManager"
