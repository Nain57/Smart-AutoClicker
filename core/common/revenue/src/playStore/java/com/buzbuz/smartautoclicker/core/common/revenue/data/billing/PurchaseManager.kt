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
package com.buzbuz.smartautoclicker.core.common.revenue.data.billing

import android.util.Log
import androidx.annotation.VisibleForTesting

import com.android.billingclient.api.Purchase
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue.BillingIssue
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue.BillingIssueReporter
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.cache.CachedPurchase
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.cache.PurchaseCache
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk.PurchasesQueryResult

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import javax.inject.Inject
import kotlin.time.Duration.Companion.days

internal class NewPurchaseManager @Inject constructor(
    private val purchaseCache: PurchaseCache,
    private val signatureVerifier: PurchaseSignatureVerifier,
    private val issueReporter: BillingIssueReporter,
) {

    private val mutex = Mutex()

    @VisibleForTesting
    internal var currentTimeMs: () -> Long = System::currentTimeMillis

    /** Purchase state of the product. */
    private val _state = MutableStateFlow(InAppPurchaseState.NOT_PURCHASED)
    val state: StateFlow<InAppPurchaseState> = _state

    /** Grant the last confirmed purchase until Google Play can be reached. */
    suspend fun restoreCachedPurchase() {
        mutex.withLock {
            if (_state.value != InAppPurchaseState.NOT_PURCHASED) return
            if (getValidCachedPurchase() == null) return

            Log.i(TAG, "Restoring cached purchase")
            _state.emit(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED)
        }
    }

    suspend fun handlePurchasesQueryResult(result: PurchasesQueryResult, ackQuery: suspend (Purchase) -> Boolean) {
        mutex.withLock {
            when (result) {
                is PurchasesQueryResult.Failure -> {
                    Log.w(TAG, "Purchases query failed with code ${result.responseCode}, keeping state ${_state.value}")
                    issueReporter.report(BillingIssue.PURCHASES_QUERY_FAILED, result.responseCode, result.debugMessage)
                }

                is PurchasesQueryResult.Success ->
                    if (result.purchase != null) handlePurchase(result.purchase, ackQuery)
                    else handleMissingPurchase()
            }
        }
    }

    suspend fun handleUiFlowPurchase(purchase: Purchase?, ackQuery: suspend (Purchase) -> Boolean) {
        mutex.withLock {
            if (purchase == null) {
                Log.i(TAG, "Product purchase not yet available")
                return
            }

            handlePurchase(purchase, ackQuery)
        }
    }

    private suspend fun handlePurchase(purchase: Purchase, ackQuery: suspend (Purchase) -> Boolean) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            Log.i(TAG, "Product purchase state: ${purchase.purchaseState}")
            _state.emit(purchase.toPurchaseState())
            return
        }

        if (!signatureVerifier.isValid(purchase.originalJson, purchase.signature)) {
            Log.e(TAG, "Invalid signature. Check to make sure your public key is correct.")
            issueReporter.report(BillingIssue.INVALID_SIGNATURE)
            if (getValidCachedPurchase() == null) _state.emit(InAppPurchaseState.ERROR)
            return
        }

        if (!purchase.isAcknowledged) {
            Log.i(TAG, "Product purchase state: PURCHASED")
            _state.emit(InAppPurchaseState.PURCHASED)

            if (!ackQuery(purchase)) return
            Log.i(TAG, "Product purchase state: PURCHASED_AND_ACKNOWLEDGED")
        }

        _state.emit(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED)
        cachePurchase(purchase)
    }

    /**
     * Google Play can wrongly report a purchased product as not owned when its local cache is broken. If we have a
     * confirmed purchase, only revoke it once Play has consistently reported it missing for [MISSING_PURCHASE_GRACE_MS].
     */
    private suspend fun handleMissingPurchase() {
        val cachedPurchase = getValidCachedPurchase()
        if (cachedPurchase == null) {
            Log.i(TAG, "Product purchase not found")
            _state.emit(InAppPurchaseState.NOT_PURCHASED)
            return
        }

        val now = currentTimeMs()
        val missingSince = cachedPurchase.missingSinceMs
            ?: now.also { purchaseCache.setMissingSince(it) }

        if (now - missingSince < MISSING_PURCHASE_GRACE_MS) {
            Log.w(TAG, "Product purchase not found but cached, keeping it until grace period ends")
            issueReporter.report(BillingIssue.CACHED_PURCHASE_MISSING)
            _state.emit(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED)
            return
        }

        Log.w(TAG, "Product purchase not found for too long, revoking cached purchase")
        issueReporter.report(BillingIssue.CACHED_PURCHASE_REVOKED)
        purchaseCache.clear()
        _state.emit(InAppPurchaseState.NOT_PURCHASED)
    }

    private suspend fun getValidCachedPurchase(): CachedPurchase? {
        val cachedPurchase = purchaseCache.get() ?: return null
        if (signatureVerifier.isValid(cachedPurchase.originalJson, cachedPurchase.signature)) return cachedPurchase

        Log.w(TAG, "Cached purchase signature is invalid, clearing it")
        purchaseCache.clear()
        return null
    }

    private suspend fun cachePurchase(purchase: Purchase) {
        val cachedPurchase = purchaseCache.get()
        if (cachedPurchase?.originalJson == purchase.originalJson
            && cachedPurchase.signature == purchase.signature
            && cachedPurchase.missingSinceMs == null) return

        purchaseCache.save(purchase.originalJson, purchase.signature)
    }

    private fun Purchase.toPurchaseState(): InAppPurchaseState =
        when (purchaseState) {
            Purchase.PurchaseState.UNSPECIFIED_STATE -> InAppPurchaseState.NOT_PURCHASED
            Purchase.PurchaseState.PENDING -> InAppPurchaseState.PENDING
            Purchase.PurchaseState.PURCHASED ->
                if (isAcknowledged) InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED
                else InAppPurchaseState.PURCHASED

            else -> {
                Log.e(TAG, "Purchase in unknown state: $purchaseState")
                InAppPurchaseState.ERROR
            }
        }
}

internal enum class InAppPurchaseState {
    NOT_PURCHASED,
    PENDING,
    PURCHASED,
    PURCHASED_AND_ACKNOWLEDGED,
    ERROR
}

@VisibleForTesting
internal val MISSING_PURCHASE_GRACE_MS = 3.days.inWholeMilliseconds
private const val TAG = "PurchaseManager"
