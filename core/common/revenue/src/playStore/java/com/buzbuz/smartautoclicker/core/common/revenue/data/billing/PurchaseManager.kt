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

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue.BillingIssue
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue.BillingIssueReporter
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.cache.CachedPurchase
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.cache.PurchaseCache
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk.PurchasesQueryResult

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

    /** True once Google Play answered the purchases query in this process. Until then, [state] can be wrong. */
    private val _isStateSettled = MutableStateFlow(false)
    val isStateSettled: StateFlow<Boolean> = _isStateSettled

    /** Grant the last confirmed purchase until Google Play can be reached. */
    suspend fun restoreCachedPurchase() {
        mutex.withLock {
            if (_state.value != InAppPurchaseState.NOT_PURCHASED) return
            if (getValidCachedPurchase() == null) return

            Log.i(TAG, "Restoring cached purchase")
            _state.emit(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED)
        }
    }

    /**
     * Play services are often not ready when the process starts, and can stay unreachable for a long time when the
     * process is started in background after a boot or an app update. The next refresh only happens when the user
     * opens an activity, so retry without limit until the purchase state is settled, so a purchase can be restored and
     * acknowledged before Play refunds it.
     */
    suspend fun refreshPurchasesUntilSettled(
        purchasesQuery: suspend () -> PurchasesQueryResult,
        ackQuery: suspend (Purchase) -> BillingResult,
    ) {
        retryWithBackoff { _isStateSettled.value || handlePurchasesQueryResult(purchasesQuery(), ackQuery) }
    }

    /**
     * Google Play checks the ownership on its servers for the billing flow, but answers the purchases query from a
     * local cache that can miss the purchase. Once the billing flow reported the product as already owned, retry until
     * the purchase is found, without handling it as missing in the meantime.
     */
    suspend fun refreshPurchasesUntilFound(
        purchasesQuery: suspend () -> PurchasesQueryResult,
        ackQuery: suspend (Purchase) -> BillingResult,
    ) {
        var isPurchaseMissing = false
        val isSettled = retryWithBackoff(maxAttempts = PURCHASES_QUERY_MAX_ATTEMPTS) {
            val result = purchasesQuery()
            isPurchaseMissing = result is PurchasesQueryResult.Success && result.purchase == null
            if (isPurchaseMissing) {
                Log.i(TAG, "Product is already owned but its purchase is not available yet")
                false
            } else {
                handlePurchasesQueryResult(result, ackQuery)
            }
        }

        // Query and acknowledge failures are reported on their own.
        if (!isSettled && isPurchaseMissing) {
            Log.w(TAG, "Product is already owned but its purchase was never found")
            issueReporter.report(BillingIssue.OWNED_PURCHASE_NOT_FOUND)
        }
    }

    /** Returns false if Google Play couldn't settle the purchase state and the query should be retried later. */
    suspend fun handlePurchasesQueryResult(
        result: PurchasesQueryResult,
        ackQuery: suspend (Purchase) -> BillingResult,
    ): Boolean =
        mutex.withLock {
            val isSettled = when (result) {
                is PurchasesQueryResult.Failure -> {
                    Log.w(TAG, "Purchases query failed with code ${result.responseCode}, keeping state ${_state.value}")
                    issueReporter.report(BillingIssue.PURCHASES_QUERY_FAILED, result.responseCode, result.debugMessage)
                    false
                }

                is PurchasesQueryResult.Success ->
                    if (result.purchase != null) handlePurchase(result.purchase, ackQuery)
                    else {
                        handleMissingPurchase()
                        true
                    }
            }

            if (isSettled) _isStateSettled.value = true
            isSettled
        }

    suspend fun handleUiFlowPurchase(purchase: Purchase?, ackQuery: suspend (Purchase) -> BillingResult) {
        mutex.withLock {
            if (purchase == null) {
                Log.i(TAG, "Product purchase not yet available")
                return
            }

            handlePurchase(purchase, ackQuery)
        }
    }

    /**
     * Calls [isSettled] until it returns true, or until it was called [maxAttempts] times if set. The delay between
     * each attempt doubles, up to [PURCHASES_QUERY_MAX_RETRY_DELAY]. Returns false if all attempts were made without
     * settling.
     */
    private suspend fun retryWithBackoff(maxAttempts: Int? = null, isSettled: suspend () -> Boolean): Boolean {
        var retryDelay = PURCHASES_QUERY_INITIAL_RETRY_DELAY
        var attempt = 1
        while (!isSettled()) {
            if (attempt == maxAttempts) return false

            delay(retryDelay)
            retryDelay = minOf(retryDelay * 2, PURCHASES_QUERY_MAX_RETRY_DELAY)
            attempt++
        }
        return true
    }

    /** Returns false if the purchase couldn't be acknowledged. */
    private suspend fun handlePurchase(purchase: Purchase, ackQuery: suspend (Purchase) -> BillingResult): Boolean {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            Log.i(TAG, "Product purchase state: ${purchase.purchaseState}")
            _state.emit(purchase.toPurchaseState())
            return true
        }

        if (!signatureVerifier.isValid(purchase.originalJson, purchase.signature)) {
            Log.e(TAG, "Invalid signature. Check to make sure your public key is correct.")
            issueReporter.report(BillingIssue.INVALID_SIGNATURE)
            if (getValidCachedPurchase() == null) _state.emit(InAppPurchaseState.ERROR)
            return true
        }

        if (!purchase.isAcknowledged) {
            Log.i(TAG, "Product purchase state: PURCHASED")
            // A user with a confirmed purchase must not see the paywall while Play waits for the acknowledgement.
            _state.emit(
                if (getValidCachedPurchase() != null) InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED
                else InAppPurchaseState.PURCHASED
            )

            val ackResult = ackQuery(purchase)
            if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Purchase acknowledgement failed with code ${ackResult.responseCode}")
                issueReporter.report(BillingIssue.ACKNOWLEDGE_FAILED, ackResult.responseCode, ackResult.debugMessage)
                return false
            }
            Log.i(TAG, "Product purchase state: PURCHASED_AND_ACKNOWLEDGED")
        }

        _state.emit(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED)
        cachePurchase(purchase)
        return true
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
            ?: now.also {
                purchaseCache.setMissingSince(it)
                issueReporter.report(BillingIssue.CACHED_PURCHASE_MISSING)
            }

        if (now - missingSince < MISSING_PURCHASE_GRACE_MS) {
            Log.w(TAG, "Product purchase not found but cached, keeping it until grace period ends")
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
                issueReporter.report(BillingIssue.UNKNOWN_PURCHASE_STATE, purchaseState)
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
internal val MISSING_PURCHASE_GRACE_MS = 30.days.inWholeMilliseconds
/**
 * Attempts to find a purchase reported as already owned by the billing flow. With the doubling delay, the last attempt
 * is made ~2.5 minutes after the first one.
 */
@VisibleForTesting
internal const val PURCHASES_QUERY_MAX_ATTEMPTS = 6
@VisibleForTesting
internal val PURCHASES_QUERY_INITIAL_RETRY_DELAY = 5.seconds
@VisibleForTesting
internal val PURCHASES_QUERY_MAX_RETRY_DELAY = 15.minutes
private const val TAG = "PurchaseManager"
