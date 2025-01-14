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
package com.buzbuz.smartautoclicker.feature.revenue.domain

import android.content.Context
import android.os.Build
import com.buzbuz.smartautoclicker.core.common.quality.domain.Quality

import com.buzbuz.smartautoclicker.core.common.quality.domain.QualityRepository
import com.buzbuz.smartautoclicker.feature.revenue.UserBillingState
import com.buzbuz.smartautoclicker.feature.revenue.data.ads.InterstitialAdsDataSource
import com.buzbuz.smartautoclicker.feature.revenue.data.UserConsentDataSource
import com.buzbuz.smartautoclicker.feature.revenue.data.ads.RemoteAdState
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.BillingDataSource
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.InAppPurchaseState
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.sdk.InAppProduct
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.AdState
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.PurchaseState

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


/** Tests for the [RevenueRepository]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class RevenueRepositoryTests {

    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockBillingDataSource: BillingDataSource
    @Mock private lateinit var mockUserConsentDataSource: UserConsentDataSource
    @Mock private lateinit var mockAdsDataSource: InterstitialAdsDataSource
    @Mock private lateinit var mockQualityRepository: QualityRepository

    private val userConsent = MutableStateFlow(false)
    private val adState: MutableStateFlow<RemoteAdState> = MutableStateFlow(RemoteAdState.SdkNotInitialized)
    private val purchaseState = MutableStateFlow(InAppPurchaseState.NOT_PURCHASED)
    private val product = MutableStateFlow<InAppProduct?>(null)
    private val quality: MutableStateFlow<Quality> = MutableStateFlow(Quality.High)

    private lateinit var testedBillingRepository: RevenueRepository

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        Mockito.`when`(mockUserConsentDataSource.isUserConsentingForAds).thenReturn(userConsent)
        Mockito.`when`(mockAdsDataSource.remoteAdState).thenReturn(adState)
        Mockito.`when`(mockBillingDataSource.purchaseState).thenReturn(purchaseState)
        Mockito.`when`(mockBillingDataSource.product).thenReturn(product)
        Mockito.`when`(mockQualityRepository.quality).thenReturn(quality)

        testedBillingRepository = RevenueRepository(
            mockContext,
            testDispatcher,
            mockUserConsentDataSource,
            mockAdsDataSource,
            mockBillingDataSource,
            mockQualityRepository,
        )
    }

    @After
    fun tearDown() {
        userConsent.value = false
        adState.value = RemoteAdState.SdkNotInitialized
        product.value = null
        purchaseState.value = InAppPurchaseState.NOT_PURCHASED
        quality.value = Quality.High
    }

    @Test
    fun `initial states`() {
        testedBillingRepository.assertStates(
            ad = AdState.NOT_INITIALIZED,
            purchase = PurchaseState.CANNOT_PURCHASE,
            billing = UserBillingState.AD_REQUESTED,
        )
    }

    @Test
    fun `can purchase`() {
        product.value = InAppProduct.Debug()
        purchaseState.value = InAppPurchaseState.NOT_PURCHASED

        testedBillingRepository.assertStates(
            ad = AdState.NOT_INITIALIZED,
            purchase = PurchaseState.NOT_PURCHASED,
            billing = UserBillingState.AD_REQUESTED,
        )
    }

    @Test
    fun `purchased product`() {
        purchaseState.value = InAppPurchaseState.PURCHASED_AND_ACKNOWLEDGED

        testedBillingRepository.assertStates(
            ad = AdState.NOT_INITIALIZED,
            purchase = PurchaseState.PURCHASED,
            billing = UserBillingState.PURCHASED,
        )
    }

    @Test
    fun `playStore billing in progress`() {
        purchaseState.value = InAppPurchaseState.PENDING

        testedBillingRepository.assertStates(
            ad = AdState.NOT_INITIALIZED,
            purchase = PurchaseState.PENDING,
            billing = UserBillingState.AD_REQUESTED,
        )
    }

    @Test
    fun `quality not high`() {
        quality.value = Quality.Crashed
        testedBillingRepository.assertStates(
            ad = AdState.NOT_INITIALIZED,
            purchase = PurchaseState.CANNOT_PURCHASE,
            billing = UserBillingState.EXEMPTED,
        )
    }

    @Test
    fun `ads initialization should not happen without user consent`() {
        verify(mockAdsDataSource, never()).initialize(mockContext)
    }

    @Test
    fun `ads initialization on user consent`() {
        adState.value = RemoteAdState.SdkNotInitialized
        userConsent.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockAdsDataSource, times(1)).initialize(mockContext)
    }

    @Test
    fun `ads initialization not twice`() {
        userConsent.value = true
        adState.value = RemoteAdState.Initialized
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockAdsDataSource, never()).initialize(mockContext)
    }

    @Test
    fun `ads watched reset`() {
        userConsent.value = true
        adState.value = RemoteAdState.Shown

        testDispatcher.scheduler.advanceTimeBy(AD_WATCHED_STATE_DURATION.inWholeMilliseconds - 1000)
        verify(mockAdsDataSource, never()).reset()
        testDispatcher.scheduler.advanceTimeBy(2000)
        verify(mockAdsDataSource, times(1)).reset()
    }

    @Test
    fun `paywall ui flow in progress`() {
        testedBillingRepository.startPaywallUiFlow(mockContext)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(testedBillingRepository.isBillingFlowInProgress.value)

        testedBillingRepository.setBillingActivityDestroyed()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(testedBillingRepository.isBillingFlowInProgress.value)
    }

    @Test
    fun `purchase ui flow in progress`() {
        testedBillingRepository.startPurchaseUiFlow(mockContext)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(testedBillingRepository.isBillingFlowInProgress.value)

        testedBillingRepository.setBillingActivityDestroyed()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(testedBillingRepository.isBillingFlowInProgress.value)
    }

    @Test
    fun `trial request`() {
        testedBillingRepository.requestTrial()

        testedBillingRepository.assertStates(
            ad = AdState.NOT_INITIALIZED,
            purchase = PurchaseState.CANNOT_PURCHASE,
            billing = UserBillingState.TRIAL,
        )
    }

    @Test
    fun `trial consume`() {
        testedBillingRepository.requestTrial()
        testDispatcher.scheduler.advanceUntilIdle()

        testedBillingRepository.consumeTrial()
        testDispatcher.scheduler.advanceUntilIdle()

        testedBillingRepository.assertStates(
            ad = AdState.NOT_INITIALIZED,
            purchase = PurchaseState.CANNOT_PURCHASE,
            billing = UserBillingState.AD_REQUESTED,
        )
    }

    private fun RevenueRepository.assertStates(ad: AdState, purchase: PurchaseState, billing: UserBillingState) {
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ad, adsState.value)
        assertEquals(purchase, purchaseState.value)
        assertEquals(billing, userBillingState.value)
    }
}