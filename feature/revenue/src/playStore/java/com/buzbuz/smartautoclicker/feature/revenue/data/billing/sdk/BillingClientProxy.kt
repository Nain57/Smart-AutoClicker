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

package com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk

import android.app.Activity
import android.content.Context
import android.util.Log

import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.querySkuDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


internal class BillingClientProxy(
    context: Context,
    private val productId: String,
    private val onPurchaseStateFromUiFlow: (Purchase?) -> Unit,
) {

    internal val client = BillingClient.newBuilder(context)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener { result, purchases ->
            billingUiFlowResultListener?.invoke(
                result,
                purchases?.findProductPurchase(productId)
            )
        }
        .build()

    private var billingUiFlowResultListener: ((BillingResult, Purchase?) -> Unit)? = null


    fun launchBillingFlow(activity: Activity, product: InAppProduct): StateFlow<BillingUiFlowState> {
        val billingUiFlowState: MutableStateFlow<BillingUiFlowState> =
            MutableStateFlow(BillingUiFlowState.NOT_VISIBLE_CANNOT_SHOW)

        if (!client.isReady) return billingUiFlowState

        billingUiFlowResultListener = { result, purchase ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Log.i(TAG, "onPurchasesUpdated: OK")
                    billingUiFlowState.value = BillingUiFlowState.NOT_VISIBLE
                    onPurchaseStateFromUiFlow(purchase)
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    Log.i(TAG, "onPurchasesUpdated: User canceled the purchase. ${result.toLogString()}")
                    billingUiFlowState.value = BillingUiFlowState.NOT_VISIBLE
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    Log.i(TAG, "onPurchasesUpdated: The user already owns this item. ${result.toLogString()}")
                    billingUiFlowState.value = BillingUiFlowState.NOT_VISIBLE_ALREADY_OWNED
                }
                BillingClient.BillingResponseCode.DEVELOPER_ERROR -> {
                    Log.e(TAG, "onPurchasesUpdated: Google Play \"does not recognize the configuration. ${result.toLogString()}")
                    billingUiFlowState.value = BillingUiFlowState.NOT_VISIBLE_ERROR
                }
                else -> {
                    Log.d(TAG, "onPurchasesUpdated: Error ${result.toLogString()}")
                    billingUiFlowState.value = BillingUiFlowState.NOT_VISIBLE_ERROR
                }
            }

            billingUiFlowResultListener = null
        }

        Log.i(TAG, "Launch PlayStore billing ui flow")

        val startResult = client.launchBillingFlow(
            activity,
            when (product) {
                is InAppProduct.Modern -> billingFlowQueryParams(product.productDetails)
                is InAppProduct.Legacy -> legacyBillingFlowQueryParams(product.productDetails)
            },
        )

        if (startResult.responseCode == BillingClient.BillingResponseCode.OK) {
            billingUiFlowState.value = BillingUiFlowState.VISIBLE
        } else {
            Log.e(TAG, "Error while starting play store billing ui flow. ${startResult.toLogString()}")
        }

        return billingUiFlowState
    }

    suspend fun fetchInAppPurchases(): Purchase? {
        if (!client.isReady) {
            Log.w(TAG, "Cannot refresh purchases, client is not ready.")
            return null
        }

        Log.i(TAG, "Refreshing purchases")

        val result = client.queryPurchasesAsync(inAppProductsPurchasesQueryParams())
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            Log.i(TAG, "fetchInAppPurchases:  OK")
            val purchase = result.purchasesList.findProductPurchase(productId)
            if (purchase != null) return purchase

            Log.d(TAG, "fetchInAppPurchases: Null Purchase List Returned from OK response!")
        } else {
            Log.i(TAG, "fetchInAppPurchases error: ${result.billingResult.toLogString()}")
        }

        return null
    }

    suspend fun fetchInAppProductDetails(): Pair<Boolean, InAppProduct?> {
        if (!client.isReady) return (false to null)

        Log.i(TAG, "Refreshing product details")

        val (billingResult, product) = fetchInAppProductDetailsCompat()
            ?: return (false to null)

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.i(TAG, "fetchInAppProductDetails OK")

                if (product == null) Log.e(TAG, "fetchInAppProductDetails: Found null or empty ProductDetails.")
                return ((product != null) to product)
            }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.i(TAG, "fetchInAppProductDetails: ${billingResult.toLogString()}")

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            BillingClient.BillingResponseCode.ERROR ->
                Log.e(TAG, "fetchInAppProductDetails: ${billingResult.toLogString()}")

            else -> Log.wtf(TAG, "fetchInAppProductDetails: ${billingResult.toLogString()}")
        }

        return (false to product)
    }

    suspend fun acknowledgePurchase(purchase: Purchase): Boolean {
        if (purchase.isAcknowledged) {
            Log.d(TAG, "Purchase is already acknowledged")
            return true
        }

        Log.i(TAG, "Acknowledging purchase")

        val billingResult = client.acknowledgePurchase(acknowledgePurchaseQueryParams(purchase.purchaseToken))
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Error acknowledging purchase. ${billingResult.toLogString()}")
            return false
        }

        Log.i(TAG, "Purchase acknowledged")

        return true
    }

    private suspend fun fetchInAppProductDetailsCompat(): Pair<BillingResult, InAppProduct?>? =
        when (val code = client.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS).responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val result = client.queryProductDetails(inAppProductDetailsQueryParams(productId))
                result.billingResult to result.productDetailsList?.findProduct(productId)
            }

            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                Log.d(TAG, "Product Details feature not supported, using legacy sku methods")

                @Suppress("DEPRECATION")
                val result = client.querySkuDetails(legacyInAppProductDetailsQueryParams(productId))
                result.billingResult to result.skuDetailsList?.findLegacyProduct(productId)
            }

            else -> {
                Log.w(TAG, "Error while checking feature flags for product details: $code")
                null
            }
        }
}

private fun billingFlowQueryParams(product: ProductDetails) =
    BillingFlowParams.newBuilder()
        .setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(product)
                    .build()
            )
        )
        .build()

private fun inAppProductDetailsQueryParams(productId: String) =
    QueryProductDetailsParams.newBuilder()
        .setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
        )
        .build()

private fun inAppProductsPurchasesQueryParams() =
    QueryPurchasesParams.newBuilder()
        .setProductType(BillingClient.ProductType.INAPP)
        .build()

private fun acknowledgePurchaseQueryParams(purchaseToken: String) =
    AcknowledgePurchaseParams.newBuilder()
        .setPurchaseToken(purchaseToken)
        .build()

private fun List<Purchase>.findProductPurchase(productId: String): Purchase? =
    find { it.products.contains(productId) }

private fun List<ProductDetails>.findProduct(productId: String): InAppProduct? =
    find { it.productId == productId }?.toInAppProduct()


/** Tag for logs */
private const val TAG = "BillingClientProxy"