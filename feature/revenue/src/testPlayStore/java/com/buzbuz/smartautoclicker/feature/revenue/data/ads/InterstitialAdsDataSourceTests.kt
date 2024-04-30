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
package com.buzbuz.smartautoclicker.feature.revenue.data.ads

import android.app.Activity
import android.content.Context
import android.os.Build

import com.buzbuz.smartautoclicker.feature.revenue.data.ads.sdk.IAdsSdk

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config


/** Tests for the [InterstitialAdsDataSource]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class InterstitialAdsDataSourceTests {

    private val testDispatcher = StandardTestDispatcher()

    @Mock private lateinit var mockActivity: Activity
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockAdsSdk: IAdsSdk

    private lateinit var testedDataSource: InterstitialAdsDataSource

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        doAnswer { invocation -> invocation.getArgument<(() -> Unit)>(1).invoke() }
            .`when`(mockAdsSdk).initializeSdk(eq(mockContext), any())

        testedDataSource = InterstitialAdsDataSource(
            context = mockContext,
            mainDispatcher = testDispatcher,
            adsSdk = mockAdsSdk,
        )
    }

    @Test
    fun `initial state`() {
        assertEquals(RemoteAdState.SdkNotInitialized, testedDataSource.remoteAdState.value)
    }

    @Test
    fun `initialize sdk`() {
        testedDataSource.initializeAndWaitForCoroutines()

        assertEquals(RemoteAdState.Initialized, testedDataSource.remoteAdState.value)
    }

    @Test
    fun `load initialized`() {
        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()

        assertEquals(RemoteAdState.Loading, testedDataSource.remoteAdState.value)
        verify(mockAdsSdk).loadInterstitialAd(eq(mockContext), any(), any())
    }

    @Test
    fun `load pending execution`() {
        testedDataSource.loadAdAndWaitForCoroutines()

        // Ensure the loading request doesn't change the ad state
        assertEquals(RemoteAdState.SdkNotInitialized, testedDataSource.remoteAdState.value)

        testedDataSource.initializeAndWaitForCoroutines()

        // Previous loading request should be executed automatically
        assertEquals(RemoteAdState.Loading, testedDataSource.remoteAdState.value)
        verify(mockAdsSdk, times(1)).loadInterstitialAd(eq(mockContext), any(), any())
    }

    @Test
    fun `load complete`() {
        mockAdsSdk.mockLoadImmediateCompletion()

        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()

        assertEquals(RemoteAdState.NotShown, testedDataSource.remoteAdState.value)
    }

    @Test
    fun `load other states`() {
        mockAdsSdk.mockLoadImmediateCompletion()

        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()

        assertEquals(RemoteAdState.NotShown, testedDataSource.remoteAdState.value)
        verify(mockAdsSdk, times(1)).loadInterstitialAd(eq(mockContext), any(), any())
    }

    @Test
    fun `load retry and error`() {
        val errorCode = 1
        val errorMessage = "error"
        mockAdsSdk.mockLoadImmediateFailure(errorCode, errorMessage)

        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()

        assertEquals(RemoteAdState.Error.LoadingError(errorCode, errorMessage), testedDataSource.remoteAdState.value)
        verify(mockAdsSdk, times(MAX_LOADING_RETRIES)).loadInterstitialAd(eq(mockContext), any(), any())
    }

    @Test
    fun `show loaded`() {
        mockAdsSdk.mockLoadImmediateCompletion()

        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()
        testedDataSource.showAdAndWaitForCoroutines()

        verify(mockAdsSdk, times(1)).showInterstitialAd(eq(mockActivity), any(), any(), any())
    }

    @Test
    fun `show other states`() {
        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.showAdAndWaitForCoroutines()

        verify(mockAdsSdk, never()).showInterstitialAd(eq(mockActivity), any(), any(), any())
    }

    @Test
    fun `show success`() {
        mockAdsSdk.mockLoadImmediateCompletion()
        mockAdsSdk.mockShown()

        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()
        testedDataSource.showAdAndWaitForCoroutines()

        assertEquals(RemoteAdState.Showing, testedDataSource.remoteAdState.value)
    }

    @Test
    fun `shown dismiss with impression`() {
        mockAdsSdk.mockLoadImmediateCompletion()
        mockAdsSdk.mockShown()
        mockAdsSdk.mockDismissed(impression = true)

        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()
        testedDataSource.showAdAndWaitForCoroutines()

        assertEquals(RemoteAdState.Shown, testedDataSource.remoteAdState.value)
    }

    @Test
    fun `shown dismiss without impression`() {
        mockAdsSdk.mockLoadImmediateCompletion()
        mockAdsSdk.mockShown()
        mockAdsSdk.mockDismissed(impression = false)

        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()
        testedDataSource.showAdAndWaitForCoroutines()

        assertEquals(RemoteAdState.Error.NoImpressionError, testedDataSource.remoteAdState.value)
    }

    @Test
    fun `shown error`() {
        val errorCode = 1
        val errorMessage = "error"
        mockAdsSdk.mockLoadImmediateCompletion()
        mockAdsSdk.mockShown()
        mockAdsSdk.mockShowError(errorCode, errorMessage)

        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()
        testedDataSource.showAdAndWaitForCoroutines()

        assertEquals(RemoteAdState.Error.ShowError(errorCode, errorMessage), testedDataSource.remoteAdState.value)
    }

    @Test
    fun `reset from sdk not initialized`() {
        testedDataSource.resetAndWaitForCoroutines()
        assertEquals(RemoteAdState.SdkNotInitialized, testedDataSource.remoteAdState.value)
    }

    @Test
    fun `reset from other`() {
        testedDataSource.initializeAndWaitForCoroutines()
        testedDataSource.loadAdAndWaitForCoroutines()

        testedDataSource.resetAndWaitForCoroutines()

        assertEquals(RemoteAdState.Initialized, testedDataSource.remoteAdState.value)
    }

    private fun InterstitialAdsDataSource.initializeAndWaitForCoroutines() {
        initialize(mockContext)
        testDispatcher.scheduler.advanceUntilIdle()
    }
    private fun InterstitialAdsDataSource.loadAdAndWaitForCoroutines() {
        loadAd(mockContext)
        testDispatcher.scheduler.advanceUntilIdle()
    }
    private fun InterstitialAdsDataSource.showAdAndWaitForCoroutines() {
        showAd(mockActivity)
        testDispatcher.scheduler.advanceUntilIdle()
    }
    private fun InterstitialAdsDataSource.resetAndWaitForCoroutines() {
        reset()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun IAdsSdk.mockLoadImmediateCompletion() {
        doAnswer { invocation ->
            invocation.getArgument<(() -> Unit)>(1).invoke()
            testDispatcher.scheduler.advanceUntilIdle()
        }.`when`(this).loadInterstitialAd(eq(mockContext), any(), any())
    }

    private fun IAdsSdk.mockLoadImmediateFailure(code: Int, message: String) {
        doAnswer { invocation ->
            invocation.getArgument<((Int, String) -> Unit)>(2).invoke(code, message)
            testDispatcher.scheduler.advanceUntilIdle()
        }.`when`(this).loadInterstitialAd(eq(mockContext), any(), any())
    }

    private fun IAdsSdk.mockShown() {
        doAnswer { invocation ->
            invocation.getArgument<(() -> Unit)>(1).invoke()
            testDispatcher.scheduler.advanceUntilIdle()
        }.`when`(this).showInterstitialAd(eq(mockActivity), any(), any(), any())
    }

    private fun IAdsSdk.mockDismissed(impression: Boolean) {
        doAnswer { invocation ->
            invocation.getArgument<((Boolean) -> Unit)>(2).invoke(impression)
            testDispatcher.scheduler.advanceUntilIdle()
        }.`when`(this).showInterstitialAd(eq(mockActivity), any(), any(), any())
    }

    private fun IAdsSdk.mockShowError(code: Int, message: String) {
        doAnswer { invocation ->
            invocation.getArgument<((Int, String) -> Unit)>(3).invoke(code, message)
            testDispatcher.scheduler.advanceUntilIdle()
        }.`when`(this).showInterstitialAd(eq(mockActivity), any(), any(), any())
    }
}