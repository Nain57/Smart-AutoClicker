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

import android.util.Log
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import javax.inject.Inject

internal class NewPurchaseManager @Inject constructor() {


    /** Purchase state of the product. */
    private val _state = MutableStateFlow(InAppPurchaseState.NOT_PURCHASED)
    val state: StateFlow<InAppPurchaseState> = _state

    suspend fun handleNewPurchases(
        purchase: Purchase?,
        fromFetch: Boolean,
        ackQuery: suspend (Purchase) -> Boolean,
    ) {
        // Clear purchase state of anything that didn't come with this purchase list if this is part of a refresh query.
        if (fromFetch && purchase == null) {
            Log.i(TAG, "Product purchase not found")
            _state.emit(InAppPurchaseState.NOT_PURCHASED)
            return
        }

        if (purchase == null) return

        val purchaseState = purchase.purchaseState
        if (purchaseState != Purchase.PurchaseState.PURCHASED) {
            Log.i(TAG, "Product purchase state: ${purchase.purchaseState}")
            _state.emit(purchase.toPurchaseState())
            return
        }

        // Before going further, check if the PURCHASE state is legit
        if (!isSignatureValid(purchase)) {
            Log.e(TAG, "Invalid signature. Check to make sure your public key is correct.")
            _state.emit(InAppPurchaseState.ERROR)
            return
        }

        // If this is a new purchase, acknowledge it
        if (!purchase.isAcknowledged) {
            _state.emit(InAppPurchaseState.PURCHASED)
            if (ackQuery(purchase)) _state.emit(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED)
            return
        }

        _state.emit(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED)
    }


    private fun Purchase?.toPurchaseState(): InAppPurchaseState =
        when {
            this == null -> InAppPurchaseState.NOT_PURCHASED
            purchaseState == Purchase.PurchaseState.UNSPECIFIED_STATE -> InAppPurchaseState.NOT_PURCHASED
            purchaseState == Purchase.PurchaseState.PENDING -> InAppPurchaseState.PENDING
            purchaseState == Purchase.PurchaseState.PURCHASED ->
                if (isAcknowledged) InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED
                else InAppPurchaseState.PURCHASED

            else -> {
                Log.e(TAG, "Purchase in unknown state: $purchaseState")
                InAppPurchaseState.ERROR
            }
        }

    private fun isSignatureValid(purchase: Purchase): Boolean {
        return Security.verifyPurchase(purchase.originalJson, purchase.signature)
    }
}

internal enum class InAppPurchaseState {
    NOT_PURCHASED,
    PENDING,
    PURCHASED,
    PURCHASED_AND_ACKNOWLEDGED,
    ERROR
}

private const val TAG = "PurchaseManager"