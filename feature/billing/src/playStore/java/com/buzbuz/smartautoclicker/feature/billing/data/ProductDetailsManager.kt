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

import android.os.SystemClock
import android.util.Log

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetails

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class ProductDetailsManager(
    private val billingClient: BillingClient,
    private val productId: String,
    defaultScope: CoroutineScope,
) {

    private val product = QueryProductDetailsParams.Product.newBuilder()
        .setProductId(productId)
        .setProductType(BillingClient.ProductType.INAPP)
        .build()

    /** Details for the product*/
    val productDetails = MutableStateFlow<ProductDetails?>(null)

    /** When was the last successful ProductDetailsResponse */
    private var productDetailsResponseTime = -PRODUCT_DETAILS_QUERY_RETRY_TIME

    init {
        productDetails.subscriptionCount
            .map { count -> count > 0 }
            .distinctUntilChanged()
            .onEach { isActive ->
                if (isActive && (SystemClock.elapsedRealtime() - productDetailsResponseTime > PRODUCT_DETAILS_QUERY_RETRY_TIME)) {
                    productDetailsResponseTime = SystemClock.elapsedRealtime()
                    Log.v(LOG_TAG, "product not fresh, requerying")
                    queryProductDetailsAsync()
                }
            }
            .launchIn(defaultScope)
    }

    /**
     * Calls the billing client functions to query sku details for the inApp SKUs.
     * SKU details are useful for displaying item names and price lists to the user, and are required to make a
     * purchase.
     */
    suspend fun queryProductDetailsAsync() {
        val productDetailsResult = billingClient.queryProductDetails(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()
        )

        onProductDetailsResponse(productDetailsResult.billingResult, productDetailsResult.productDetailsList)
    }

    /**
     * Receives the result from [queryProductDetails].
     *
     * Store the SkuDetails and post them in the [.skuDetailsMap]. This allows other
     * parts of the app to use the [ProductDetails] to show SKU information and make purchases.
     */
    private fun onProductDetailsResponse(billingResult: BillingResult, productDetailsList: List<ProductDetails>?) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage

        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.i(LOG_TAG, "onProductDetailsResponse: $responseCode $debugMessage")

                if (!productDetailsList.isNullOrEmpty()) {
                    val details = productDetailsList.first()
                    if (details.productId == productId) {
                        productDetails.tryEmit(details)
                    }
                } else {
                    Log.e(LOG_TAG, "onProductDetailsResponse: Found null or empty ProductDetails.")
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.i(LOG_TAG, "onProductDetailsResponse: $responseCode $debugMessage")

            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            BillingClient.BillingResponseCode.ERROR ->
                Log.e(LOG_TAG, "onProductDetailsResponse: $responseCode $debugMessage")

            else -> Log.wtf(LOG_TAG, "onProductDetailsResponse: $responseCode $debugMessage")
        }

        productDetailsResponseTime = if (responseCode == BillingClient.BillingResponseCode.OK) {
            SystemClock.elapsedRealtime()
        } else {
            -PRODUCT_DETAILS_QUERY_RETRY_TIME
        }
    }
}

private const val LOG_TAG = "BillingDataSource"
private const val PRODUCT_DETAILS_QUERY_RETRY_TIME = 1000L * 60L * 60L * 4L // 4 hours