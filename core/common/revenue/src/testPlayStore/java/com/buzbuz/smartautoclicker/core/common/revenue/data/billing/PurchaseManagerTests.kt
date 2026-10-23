/*
 * Copyright (C) 2026 Kevin Buzeau
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

import android.os.Build

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue.BillingIssue
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.issue.BillingIssueReporter
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.cache.CachedPurchase
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.cache.PurchaseCache
import com.buzbuz.smartautoclicker.core.common.revenue.data.billing.sdk.PurchasesQueryResult

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import kotlin.time.Duration.Companion.minutes

/** Tests for the [NewPurchaseManager]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PurchaseManagerTests {

    @Mock private lateinit var mockSignatureVerifier: PurchaseSignatureVerifier
    @Mock private lateinit var mockIssueReporter: BillingIssueReporter

    private lateinit var purchaseCache: FakePurchaseCache
    private var currentTimeMs = 0L

    private lateinit var purchaseManager: NewPurchaseManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Mockito.`when`(mockSignatureVerifier.isValid(any(), anyOrNull())).thenReturn(true)

        purchaseCache = FakePurchaseCache()
        currentTimeMs = 0L

        purchaseManager = NewPurchaseManager(purchaseCache, mockSignatureVerifier, mockIssueReporter)
        purchaseManager.currentTimeMs = { currentTimeMs }
    }

    @Test
    fun `query failure keeps purchased state`() = runTest {
        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase()), ::ackSuccess)

        purchaseManager.handlePurchasesQueryResult(queryFailure(), ::ackSuccess)

        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
        verify(mockIssueReporter).report(
            eq(BillingIssue.PURCHASES_QUERY_FAILED),
            eq(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE),
            anyOrNull(),
        )
    }

    @Test
    fun `query failure keeps not purchased state`() = runTest {
        purchaseManager.handlePurchasesQueryResult(queryFailure(), ::ackSuccess)

        assertEquals(InAppPurchaseState.NOT_PURCHASED, purchaseManager.state.value)
    }

    @Test
    fun `missing purchase without cache is not purchased`() = runTest {
        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(null), ::ackSuccess)

        assertEquals(InAppPurchaseState.NOT_PURCHASED, purchaseManager.state.value)
    }

    @Test
    fun `acknowledged purchase is cached`() = runTest {
        val purchase = purchase()
        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase), ::ackSuccess)

        assertEquals(purchase.originalJson, purchaseCache.cached?.originalJson)
        assertEquals(SIGNATURE, purchaseCache.cached?.signature)
    }

    @Test
    fun `new purchase is cached only once acknowledged`() = runTest {
        purchaseManager.handleUiFlowPurchase(purchase(acknowledged = false), ::ackFailure)

        assertEquals(InAppPurchaseState.PURCHASED, purchaseManager.state.value)
        assertNull(purchaseCache.cached)

        purchaseManager.handleUiFlowPurchase(purchase(acknowledged = false), ::ackSuccess)

        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
        assertEquals(SIGNATURE, purchaseCache.cached?.signature)
    }

    @Test
    fun `restore cached purchase`() = runTest {
        purchaseCache.cached = CachedPurchase(purchase().originalJson, SIGNATURE, null)

        purchaseManager.restoreCachedPurchase()

        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
    }

    @Test
    fun `restore cached purchase with invalid signature`() = runTest {
        purchaseCache.cached = CachedPurchase(purchase().originalJson, SIGNATURE, null)
        Mockito.`when`(mockSignatureVerifier.isValid(any(), anyOrNull())).thenReturn(false)

        purchaseManager.restoreCachedPurchase()

        assertEquals(InAppPurchaseState.NOT_PURCHASED, purchaseManager.state.value)
        assertNull(purchaseCache.cached)
    }

    @Test
    fun `restore does not override state from play`() = runTest {
        purchaseCache.cached = CachedPurchase(purchase().originalJson, SIGNATURE, null)
        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase(pending = true)), ::ackSuccess)

        purchaseManager.restoreCachedPurchase()

        assertEquals(InAppPurchaseState.PENDING, purchaseManager.state.value)
    }

    @Test
    fun `missing cached purchase is kept during grace period`() = runTest {
        purchaseCache.cached = CachedPurchase(purchase().originalJson, SIGNATURE, null)
        currentTimeMs = 1_000L

        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(null), ::ackSuccess)
        currentTimeMs += MISSING_PURCHASE_GRACE_MS - 1
        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(null), ::ackSuccess)

        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
        assertEquals(1_000L, purchaseCache.cached?.missingSinceMs)
    }

    @Test
    fun `missing cached purchase is revoked after grace period`() = runTest {
        purchaseCache.cached = CachedPurchase(purchase().originalJson, SIGNATURE, null)
        currentTimeMs = 1_000L

        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(null), ::ackSuccess)
        currentTimeMs += MISSING_PURCHASE_GRACE_MS
        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(null), ::ackSuccess)

        assertEquals(InAppPurchaseState.NOT_PURCHASED, purchaseManager.state.value)
        assertNull(purchaseCache.cached)
    }

    @Test
    fun `confirmed purchase resets grace period`() = runTest {
        purchaseCache.cached = CachedPurchase(purchase().originalJson, SIGNATURE, null)
        currentTimeMs = 1_000L

        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(null), ::ackSuccess)
        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase()), ::ackSuccess)

        assertNull(purchaseCache.cached?.missingSinceMs)
    }

    @Test
    fun `invalid signature from play keeps cached purchase`() = runTest {
        purchaseCache.cached = CachedPurchase(CACHED_JSON, SIGNATURE, null)
        Mockito.`when`(mockSignatureVerifier.isValid(eq(CACHED_JSON), anyOrNull())).thenReturn(true)
        purchaseManager.restoreCachedPurchase()
        Mockito.`when`(mockSignatureVerifier.isValid(eq(purchase().originalJson), anyOrNull())).thenReturn(false)

        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase()), ::ackSuccess)

        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
    }

    @Test
    fun `invalid signature from play without cache is error`() = runTest {
        Mockito.`when`(mockSignatureVerifier.isValid(any(), anyOrNull())).thenReturn(false)

        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase()), ::ackSuccess)

        assertEquals(InAppPurchaseState.ERROR, purchaseManager.state.value)
    }


    @Test
    fun `query result is settled on success`() = runTest {
        assertTrue(purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase()), ::ackSuccess))
        assertTrue(purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(null), ::ackSuccess))
    }

    @Test
    fun `query result is not settled on query failure`() = runTest {
        assertFalse(purchaseManager.handlePurchasesQueryResult(queryFailure(), ::ackSuccess))
    }

    @Test
    fun `state is settled once play answered the query`() = runTest {
        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(null), ::ackSuccess)

        assertTrue(purchaseManager.isStateSettled.value)
    }

    @Test
    fun `state is not settled by a cached purchase or a query failure`() = runTest {
        purchaseCache.cached = CachedPurchase(purchase().originalJson, SIGNATURE, null)

        purchaseManager.restoreCachedPurchase()
        purchaseManager.handlePurchasesQueryResult(queryFailure(), ::ackSuccess)

        assertFalse(purchaseManager.isStateSettled.value)
    }

    @Test
    fun `acknowledge failure is reported and not settled`() = runTest {
        val isSettled = purchaseManager.handlePurchasesQueryResult(
            PurchasesQueryResult.Success(purchase(acknowledged = false)),
            ::ackFailure,
        )

        assertFalse(isSettled)
        assertEquals(InAppPurchaseState.PURCHASED, purchaseManager.state.value)
        verify(mockIssueReporter).report(
            eq(BillingIssue.ACKNOWLEDGE_FAILED),
            eq(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE),
            anyOrNull(),
        )
    }

    @Test
    fun `unacknowledged purchase with cache keeps purchased state when acknowledge fails`() = runTest {
        purchaseCache.cached = CachedPurchase(purchase().originalJson, SIGNATURE, null)

        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase(acknowledged = false)), ::ackFailure)

        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
        assertEquals(SIGNATURE, purchaseCache.cached?.signature)
    }

    @Test
    fun `unacknowledged purchase with invalid cache is purchased when acknowledge fails`() = runTest {
        purchaseCache.cached = CachedPurchase(CACHED_JSON, SIGNATURE, null)
        Mockito.`when`(mockSignatureVerifier.isValid(eq(CACHED_JSON), anyOrNull())).thenReturn(false)

        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase(acknowledged = false)), ::ackFailure)

        assertEquals(InAppPurchaseState.PURCHASED, purchaseManager.state.value)
        assertNull(purchaseCache.cached)
    }


    @Test
    fun `refresh until settled queries once when settled`() = runTest {
        val queryTimesMs = mutableListOf<Long>()

        purchaseManager.refreshPurchasesUntilSettled(
            purchasesQuery = { queryTimesMs.add(currentTime); PurchasesQueryResult.Success(purchase()) },
            ackQuery = ::ackSuccess,
        )

        assertEquals(listOf(0L), queryTimesMs)
        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
    }

    @Test
    fun `refresh until settled retries query failures with doubling delay`() = runTest {
        val results = ArrayDeque(listOf(queryFailure(), queryFailure(), PurchasesQueryResult.Success(purchase())))
        val queryTimesMs = mutableListOf<Long>()

        purchaseManager.refreshPurchasesUntilSettled(
            purchasesQuery = { queryTimesMs.add(currentTime); results.removeFirst() },
            ackQuery = ::ackSuccess,
        )

        val initialDelayMs = PURCHASES_QUERY_INITIAL_RETRY_DELAY.inWholeMilliseconds
        assertEquals(listOf(0L, initialDelayMs, initialDelayMs * 3), queryTimesMs)
        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
    }

    @Test
    fun `refresh until settled retries acknowledge failures`() = runTest {
        val ackResults = ArrayDeque(listOf(ackFailure(purchase()), ackSuccess(purchase())))
        var queryCount = 0

        purchaseManager.refreshPurchasesUntilSettled(
            purchasesQuery = { queryCount++; PurchasesQueryResult.Success(purchase(acknowledged = false)) },
            ackQuery = { ackResults.removeFirst() },
        )

        assertEquals(2, queryCount)
        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
        assertEquals(SIGNATURE, purchaseCache.cached?.signature)
    }

    @Test
    fun `refresh until settled retries without limit with a capped delay`() = runTest {
        val failureCount = PURCHASES_QUERY_MAX_ATTEMPTS * 2
        val results = ArrayDeque(List(failureCount) { queryFailure() } + PurchasesQueryResult.Success(purchase()))
        val queryTimesMs = mutableListOf<Long>()

        purchaseManager.refreshPurchasesUntilSettled(
            purchasesQuery = { queryTimesMs.add(currentTime); results.removeFirst() },
            ackQuery = ::ackSuccess,
        )

        val retryDelaysMs = queryTimesMs.zipWithNext { previous, next -> next - previous }
        val maxRetryDelayMs = PURCHASES_QUERY_MAX_RETRY_DELAY.inWholeMilliseconds
        assertEquals(failureCount + 1, queryTimesMs.size)
        assertEquals(maxRetryDelayMs, retryDelaysMs.max())
        assertEquals(maxRetryDelayMs, retryDelaysMs.last())
        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
    }

    @Test
    fun `refresh until settled stops once another query settled the state`() = runTest {
        var queryCount = 0
        val refreshJob = launch {
            purchaseManager.refreshPurchasesUntilSettled(
                purchasesQuery = { queryCount++; queryFailure() },
                ackQuery = ::ackSuccess,
            )
        }
        runCurrent()

        purchaseManager.handlePurchasesQueryResult(PurchasesQueryResult.Success(purchase()), ::ackSuccess)
        advanceUntilIdle()

        assertEquals(1, queryCount)
        assertTrue(refreshJob.isCompleted)
    }

    @Test
    fun `refresh until settled restores uncached purchase when play is unreachable for minutes`() = runTest {
        val playUnreachableDurationMs = 10.minutes.inWholeMilliseconds

        purchaseManager.refreshPurchasesUntilSettled(
            purchasesQuery = {
                if (currentTime < playUnreachableDurationMs) queryFailure()
                else PurchasesQueryResult.Success(purchase())
            },
            ackQuery = ::ackSuccess,
        )

        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
        assertEquals(SIGNATURE, purchaseCache.cached?.signature)
    }


    @Test
    fun `refresh until found retries until the purchase is found with doubling delay`() = runTest {
        val results = ArrayDeque(listOf(
            PurchasesQueryResult.Success(null),
            queryFailure(),
            PurchasesQueryResult.Success(purchase()),
        ))
        val queryTimesMs = mutableListOf<Long>()

        purchaseManager.refreshPurchasesUntilFound(
            purchasesQuery = { queryTimesMs.add(currentTime); results.removeFirst() },
            ackQuery = ::ackSuccess,
        )

        val initialDelayMs = PURCHASES_QUERY_INITIAL_RETRY_DELAY.inWholeMilliseconds
        assertEquals(listOf(0L, initialDelayMs, initialDelayMs * 3), queryTimesMs)
        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
        assertEquals(SIGNATURE, purchaseCache.cached?.signature)
    }

    @Test
    fun `refresh until found does not handle not found purchase as missing`() = runTest {
        val cachedPurchase = CachedPurchase(purchase().originalJson, SIGNATURE, null)
        purchaseCache.cached = cachedPurchase
        purchaseManager.restoreCachedPurchase()

        purchaseManager.refreshPurchasesUntilFound(
            purchasesQuery = { PurchasesQueryResult.Success(null) },
            ackQuery = ::ackSuccess,
        )

        assertEquals(InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED, purchaseManager.state.value)
        assertEquals(cachedPurchase, purchaseCache.cached)
        verify(mockIssueReporter, never()).report(eq(BillingIssue.CACHED_PURCHASE_MISSING), anyOrNull(), anyOrNull())
    }

    @Test
    fun `refresh until found gives up after max attempts`() = runTest {
        var queryCount = 0

        purchaseManager.refreshPurchasesUntilFound(
            purchasesQuery = { queryCount++; PurchasesQueryResult.Success(null) },
            ackQuery = ::ackSuccess,
        )

        assertEquals(PURCHASES_QUERY_MAX_ATTEMPTS, queryCount)
        assertEquals(InAppPurchaseState.NOT_PURCHASED, purchaseManager.state.value)
    }


    @Suppress("UNUSED_PARAMETER")
    private fun ackSuccess(purchase: Purchase): BillingResult =
        BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.OK).build()

    @Suppress("UNUSED_PARAMETER")
    private fun ackFailure(purchase: Purchase): BillingResult =
        BillingResult.newBuilder()
            .setResponseCode(BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE)
            .setDebugMessage("Service unavailable")
            .build()

    private fun queryFailure() = PurchasesQueryResult.Failure(
        responseCode = BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        debugMessage = "Service unavailable",
    )

    private fun purchase(acknowledged: Boolean = true, pending: Boolean = false): Purchase =
        Purchase(
            """{"orderId":"GPA.0000","packageName":"com.buzbuz.smartautoclicker","productId":"sac_pro",""" +
                    """"purchaseTime":1,"purchaseState":${if (pending) 4 else 0},"purchaseToken":"token",""" +
                    """"acknowledged":$acknowledged}""",
            SIGNATURE,
        )

    private class FakePurchaseCache : PurchaseCache {

        var cached: CachedPurchase? = null

        override suspend fun get(): CachedPurchase? = cached

        override suspend fun save(originalJson: String, signature: String) {
            cached = CachedPurchase(originalJson, signature, null)
        }

        override suspend fun setMissingSince(timestampMs: Long) {
            cached = cached?.copy(missingSinceMs = timestampMs)
        }

        override suspend fun clear() {
            cached = null
        }
    }
}

private const val SIGNATURE = "signature"
private const val CACHED_JSON = """{"productId":"sac_pro","cached":true}"""
