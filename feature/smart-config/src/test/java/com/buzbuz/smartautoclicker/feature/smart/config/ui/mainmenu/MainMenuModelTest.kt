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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.mainmenu

import android.content.Context
import android.os.Build

import com.buzbuz.smartautoclicker.core.common.navigation.RevenueNavigator
import com.buzbuz.smartautoclicker.core.common.navigation.getRevenueNavigator
import com.buzbuz.smartautoclicker.core.common.revenue.domain.IRevenueRepository
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.UserBillingState
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.processing.domain.SmartProcessingRepository
import com.buzbuz.smartautoclicker.core.processing.domain.model.DetectionState
import com.buzbuz.smartautoclicker.core.smart.debugging.domain.DebuggingRepository
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import kotlin.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class MainMenuModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val userBillingState = MutableStateFlow(UserBillingState.AD_REQUESTED)

    private val mockContext: Context = mockk()
    private val mockRevenueNavigator: RevenueNavigator = mockk(relaxed = true)
    private val mockRevenueRepository: IRevenueRepository = mockk(relaxed = true) {
        every { userBillingState } returns this@MainMenuModelTest.userBillingState
        every { paywallFinished } returns MutableSharedFlow()
    }
    private val mockSmartProcessingRepository: SmartProcessingRepository = mockk(relaxed = true) {
        every { scenarioId } returns MutableStateFlow(null)
        every { detectionState } returns flowOf(DetectionState.RECORDING)
        every { canStartDetection } returns flowOf(true)
    }
    private val mockEditionRepository: EditionRepository = mockk(relaxed = true) {
        every { isEditionSynchronized } returns flowOf(true)
    }
    private val mockTutorialRepository: TutorialRepository = mockk(relaxed = true) {
        every { shouldShowTip(any()) } returns flowOf(false)
        every { isTutorialStarted() } returns false
    }
    private val mockDebuggingRepository: DebuggingRepository = mockk(relaxed = true) {
        every { isLiveDebugging } returns flowOf(false)
    }

    private lateinit var viewModel: MainMenuModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(REVENUE_NAVIGATOR_FILE)
        every { any<Context>().getRevenueNavigator() } returns mockRevenueNavigator

        viewModel = MainMenuModel(
            mockSmartProcessingRepository,
            mockEditionRepository,
            mockTutorialRepository,
            NoTrialRevenueRepository(mockRevenueRepository),
            mockk(relaxed = true),
            mockDebuggingRepository,
            mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(REVENUE_NAVIGATOR_FILE)
        Dispatchers.resetMain()
    }

    @Test
    fun toggleDetection_startsDetection_whenRefreshGrantsThePurchase() {
        coEvery { mockRevenueRepository.refreshUnsettledPurchases() } answers {
            userBillingState.value = UserBillingState.PURCHASED
        }

        viewModel.toggleDetection(mockContext)

        coVerify(exactly = 1) { mockSmartProcessingRepository.startDetection(mockContext, false, false, null) }
        verify(exactly = 0) { mockRevenueNavigator.startPaywallActivity(any()) }
    }

    @Test
    fun toggleDetection_showsPaywall_whenStillNotPurchasedAfterRefresh() {
        viewModel.toggleDetection(mockContext)

        coVerify(exactly = 1) { mockRevenueRepository.refreshUnsettledPurchases() }
        verify(exactly = 1) { mockRevenueNavigator.startPaywallActivity(mockContext) }
        coVerify(exactly = 0) { mockSmartProcessingRepository.startDetection(mockContext, false, false, null) }
    }

    @Test
    fun toggleDetection_doesNotRefreshPurchases_whenAlreadyPurchased() {
        userBillingState.value = UserBillingState.PURCHASED

        viewModel.toggleDetection(mockContext)

        coVerify(exactly = 0) { mockRevenueRepository.refreshUnsettledPurchases() }
        coVerify(exactly = 1) { mockSmartProcessingRepository.startDetection(mockContext, false, false, null) }
    }

    @Test
    fun toggleDetection_ignoresToggles_whileWaitingForGooglePlay() {
        val refreshResult = CompletableDeferred<Unit>()
        coEvery { mockRevenueRepository.refreshUnsettledPurchases() } coAnswers { refreshResult.await() }

        viewModel.toggleDetection(mockContext)
        viewModel.toggleDetection(mockContext)
        refreshResult.complete(Unit)

        coVerify(exactly = 1) { mockRevenueRepository.refreshUnsettledPurchases() }
        verify(exactly = 1) { mockRevenueNavigator.startPaywallActivity(mockContext) }
    }

    /** MockK can't stub [IRevenueRepository.consumeTrial] because it returns a value class. */
    private class NoTrialRevenueRepository(revenueRepository: IRevenueRepository) : IRevenueRepository by revenueRepository {
        override fun consumeTrial(): Duration? = null
    }
}

private const val REVENUE_NAVIGATOR_FILE = "com.buzbuz.smartautoclicker.core.common.navigation.RevenueNavigatorKt"
