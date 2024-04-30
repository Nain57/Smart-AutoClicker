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

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting

import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout
import com.buzbuz.smartautoclicker.core.common.quality.Quality
import com.buzbuz.smartautoclicker.core.common.quality.QualityManager
import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.revenue.UserBillingState
import com.buzbuz.smartautoclicker.feature.revenue.data.ads.InterstitialAdsDataSource
import com.buzbuz.smartautoclicker.feature.revenue.data.ads.RemoteAdState
import com.buzbuz.smartautoclicker.feature.revenue.data.UserConsentDataSource
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.BillingDataSource
import com.buzbuz.smartautoclicker.feature.revenue.data.billing.ProModeProduct
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.AdState
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.ProModeInfo
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.PurchaseState
import com.buzbuz.smartautoclicker.feature.revenue.ui.BillingActivity
import com.buzbuz.smartautoclicker.feature.revenue.ui.paywall.PaywallFragment
import com.buzbuz.smartautoclicker.feature.revenue.ui.purchase.PurchaseProModeFragment

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes


@Singleton
internal class RevenueRepository @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(HiltCoroutineDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    private val userConsentDataSource: UserConsentDataSource,
    private val adsDataSource: InterstitialAdsDataSource,
    private val billingDataSource: BillingDataSource,
    qualityManager: QualityManager,
): IRevenueRepository, InternalRevenueRepository {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Reset the ad watched state after a while */
    private var resetAdJob: Job? = null

    private val trialRequest: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    override val proModeInfo: Flow<ProModeInfo?> =
        billingDataSource.proModeProduct.map(::toProModeInfo)

    override val isPrivacySettingRequired: Flow<Boolean> =
        userConsentDataSource.isPrivacyOptionsRequired

    override val adsState: StateFlow<AdState> =
        adsDataSource.remoteAdState.map(::toAdState)
            .stateIn(coroutineScopeIo, SharingStarted.Eagerly, AdState.NOT_INITIALIZED)

    override val purchaseState: StateFlow<PurchaseState> =
        combine(
            billingDataSource.canPurchase,
            billingDataSource.isPurchased,
            billingDataSource.billingFlowInProgress,
            ::toPurchaseState,
        ).stateIn(coroutineScopeIo, SharingStarted.Eagerly, PurchaseState.CANNOT_PURCHASE)

    override val userBillingState: StateFlow<UserBillingState> = combine(
        adsState,
        purchaseState,
        trialRequest,
        qualityManager.quality,
        ::toUserBillingState,
    ).stateIn(coroutineScopeIo, SharingStarted.Eagerly, UserBillingState.AD_REQUESTED)

    override val isBillingFlowInProgress: MutableStateFlow<Boolean> =
        MutableStateFlow(false)


    init {
        // Once the user has given his consent, initialize the ads sdk
        initAdsOnConsentFlow(context, userConsentDataSource.isUserConsentingForAds, adsState)
            .launchIn(coroutineScopeIo)

        // Some user state are temporary, monitor that and act accordingly
        userStateInvalidatorFlow(userBillingState)
            .launchIn(coroutineScopeIo)
    }


    override fun startUserConsentRequestUiFlowIfNeeded(activity: Activity) {
        if (userBillingState.value == UserBillingState.PURCHASED) return
        userConsentDataSource.requestUserConsent(activity)
    }

    override fun startPrivacySettingUiFlow(activity: Activity) {
        userConsentDataSource.showPrivacyOptionsForm(activity)
    }

    override fun loadAdIfNeeded(context: Context) {
        if (userBillingState.value != UserBillingState.AD_REQUESTED) return
        adsDataSource.loadAd(context)
    }

    override fun startPaywallUiFlow(context: Context) {
        isBillingFlowInProgress.value = true
        context.startActivity(BillingActivity.getStartIntent(context, PaywallFragment.FRAGMENT_TAG))
    }

    override fun startPurchaseUiFlow(context: Context) {
        isBillingFlowInProgress.value = true
        context.startActivity(BillingActivity.getStartIntent(context, PurchaseProModeFragment.FRAGMENT_TAG))
    }

    override fun setBillingActivityDestroyed() {
        isBillingFlowInProgress.value = false
    }

    override fun showAd(activity: Activity) {
        if (adsState.value != AdState.READY) return
        adsDataSource.showAd(activity)
    }

    override fun startPlayStoreBillingUiFlow(activity: Activity) {
        if (purchaseState.value != PurchaseState.NOT_PURCHASED) return
        billingDataSource.launchBillingFlow(activity)
    }

    override fun requestTrial() {
        Log.d(TAG, "User requesting trial period")
        trialRequest.value = true
    }

    override fun consumeTrial(): Duration? {
        if (!trialRequest.value) return null

        Log.d(TAG, "User consuming trial period")

        trialRequest.value = false
        return TRIAL_SESSION_DURATION_DURATION
    }


    private fun toProModeInfo(product: ProModeProduct?): ProModeInfo? =
        product?.let { ProModeInfo(it.title, it.description, it.price) }

    private fun toAdState(remoteAdState: RemoteAdState): AdState =
        when (remoteAdState) {
            RemoteAdState.SdkNotInitialized -> AdState.NOT_INITIALIZED
            RemoteAdState.Initialized -> AdState.INITIALIZED
            RemoteAdState.Loading -> AdState.LOADING
            is RemoteAdState.NotShown -> AdState.READY
            RemoteAdState.Showing -> AdState.SHOWING
            is RemoteAdState.Shown -> AdState.VALIDATED

            is RemoteAdState.Error.LoadingError,
            is RemoteAdState.Error.ShowError,
            RemoteAdState.Error.NoImpressionError -> AdState.ERROR
        }

    private fun toPurchaseState(canPurchase: Boolean, isPurchased: Boolean, billingInProgress: Boolean): PurchaseState =
        when {
            isPurchased -> PurchaseState.PURCHASED
            billingInProgress -> PurchaseState.BILLING_IN_PROGRESS
            canPurchase -> PurchaseState.NOT_PURCHASED
            else -> PurchaseState.CANNOT_PURCHASE
        }

    private fun toUserBillingState(adState: AdState, purchaseState: PurchaseState, trial: Boolean, quality: Quality): UserBillingState =
        when {
            purchaseState == PurchaseState.PURCHASED -> UserBillingState.PURCHASED
            quality != Quality.High -> UserBillingState.EXEMPTED
            adState == AdState.VALIDATED -> UserBillingState.AD_WATCHED
            trial -> UserBillingState.TRIAL
            else -> UserBillingState.AD_REQUESTED
        }


    private fun initAdsOnConsentFlow(context: Context, consent: Flow<Boolean>, adsState: Flow<AdState>) : Flow<Unit> =
        combine(consent, adsState) { isConsenting, state ->
            if (!isConsenting || state != AdState.NOT_INITIALIZED) return@combine

            Log.i(TAG, "User consenting for ads, initialize ads SDK")
            adsDataSource.initialize(context)
        }

    private fun userStateInvalidatorFlow(userBillingState: Flow<UserBillingState>) : Flow<Any> =
        userBillingState.onEach { state ->
            if (state != UserBillingState.AD_WATCHED) return@onEach

            resetAdJob?.cancel()
            resetAdJob = coroutineScopeIo.launch {
                Log.i(TAG, "Ad watched, starting grace delay")
                delay(AD_WATCHED_STATE_DURATION)
                Log.i(TAG, "Ad watched grace delay over, reset ad data source")

                adsDataSource.reset()
                resetAdJob = null
            }
        }


    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        super.dump(writer, prefix)
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(contentPrefix)
                .append("- userConsent=${userConsentDataSource.isUserConsentingForAds.value}; ")
                .append("adsState=${adsState.value}; ")
                .append("purchaseState=${purchaseState.value}; ")
                .println()
            append(contentPrefix)
                .append("- proModeInfo=${proModeInfo.dumpWithTimeout()}; ")
                .println()
        }
    }
}

internal val TRIAL_SESSION_DURATION_DURATION = 30.minutes
@VisibleForTesting internal val AD_WATCHED_STATE_DURATION = 1.hours

private const val TAG = "BillingRepository"