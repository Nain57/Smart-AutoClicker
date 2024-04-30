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
package com.buzbuz.smartautoclicker.feature.billing.domain

import android.app.Activity
import android.content.Context
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout
import com.buzbuz.smartautoclicker.core.common.quality.Quality
import com.buzbuz.smartautoclicker.core.common.quality.QualityManager
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.UserBillingState
import com.buzbuz.smartautoclicker.feature.billing.data.ads.InterstitialAdsDataSource
import com.buzbuz.smartautoclicker.feature.billing.data.ads.RemoteAdState
import com.buzbuz.smartautoclicker.feature.billing.data.UserConsentDataSource
import com.buzbuz.smartautoclicker.feature.billing.data.billing.BillingDataSource
import com.buzbuz.smartautoclicker.feature.billing.data.billing.ProModeProduct
import com.buzbuz.smartautoclicker.feature.billing.domain.model.AdState
import com.buzbuz.smartautoclicker.feature.billing.domain.model.ProModeInfo
import com.buzbuz.smartautoclicker.feature.billing.domain.model.PurchaseState
import com.buzbuz.smartautoclicker.feature.billing.ui.BillingActivity
import com.buzbuz.smartautoclicker.feature.billing.ui.paywall.PaywallFragment
import com.buzbuz.smartautoclicker.feature.billing.ui.purchase.PurchaseProModeFragment

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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


@Singleton
internal class BillingRepository @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(HiltCoroutineDispatchers.IO) ioDispatcher: CoroutineDispatcher,
    private val userConsentDataSource: UserConsentDataSource,
    private val adsDataSource: InterstitialAdsDataSource,
    private val billingDataSource: BillingDataSource,
    qualityManager: QualityManager,
): IBillingRepository, InternalBillingRepository {

    private val coroutineScopeIo: CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    /** Reset the ad watched state after a while */
    private var resetAdJob: Job? = null

    override val proModeInfo: Flow<ProModeInfo?> =
        billingDataSource.proModeProduct.map(::toProModeInfo)

    override val isPrivacySettingRequired: Flow<Boolean> =
        userConsentDataSource.isPrivacyOptionsRequired

    override val adsState: StateFlow<AdState> =
        combine(
            userConsentDataSource.isUserConsentingForAds,
            adsDataSource.remoteAdState,
            ::toAdState,
        ).stateIn(coroutineScopeIo, SharingStarted.Eagerly, AdState.NOT_INITIALIZED)

    override val purchaseState: StateFlow<PurchaseState> =
        combine(
            billingDataSource.canPurchase,
            billingDataSource.isPurchased,
            billingDataSource.billingFlowInProgress,
            ::toPurchaseState,
        ).stateIn(coroutineScopeIo, SharingStarted.Eagerly, PurchaseState.NOT_PURCHASED)

    override val userBillingState: StateFlow<UserBillingState> = combine(
        adsState,
        purchaseState,
        qualityManager.quality,
        ::toUserBillingState,
    ).stateIn(coroutineScopeIo, SharingStarted.Eagerly, UserBillingState.AD_REQUESTED)

    init {
        initAdsOnConsentFlow(context, userConsentDataSource.isUserConsentingForAds, adsState)
            .launchIn(coroutineScopeIo)
        watchedAdInvalidatorFlow(userBillingState)
            .launchIn(coroutineScopeIo)
    }

    override fun startUserConsentRequestUiFlowIfNeeded(activity: Activity) {
        if (userBillingState.value == UserBillingState.PURCHASED) return
        userConsentDataSource.requestUserConsent(activity)
    }

    override fun startPrivacySettingUiFlow(activity: Activity) {
        userConsentDataSource.showPrivacyOptionsForm(activity)
    }

    override fun loadAd(context: Context) {
        if (userBillingState.value != UserBillingState.AD_REQUESTED) return
        adsDataSource.loadAd(context)
    }

    override fun startPaywallUiFlow(context: Context) {
        context.startActivity(BillingActivity.getStartIntent(context, PaywallFragment.FRAGMENT_TAG))
    }

    override fun startPurchaseUiFlow(context: Context) {
        context.startActivity(BillingActivity.getStartIntent(context, PurchaseProModeFragment.FRAGMENT_TAG))
    }

    override fun showAd(activity: Activity) {
        if (adsState.value != AdState.READY) return
        adsDataSource.showAd(activity)
    }

    override fun startPlayStoreBillingUiFlow(activity: Activity) {
        if (purchaseState.value != PurchaseState.NOT_PURCHASED) return
        billingDataSource.launchBillingFlow(activity)
    }


    private fun toProModeInfo(product: ProModeProduct?): ProModeInfo? =
        product?.let { ProModeInfo(it.title, it.description, it.price) }

    private fun toAdState(isConsenting: Boolean, remoteAdState: RemoteAdState): AdState {
        if (!isConsenting) return AdState.NOT_INITIALIZED

        return when (remoteAdState) {
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
    }

    private fun toPurchaseState(canPurchase: Boolean, isPurchased: Boolean, billingInProgress: Boolean): PurchaseState =
        when {
            isPurchased -> PurchaseState.PURCHASED
            billingInProgress -> PurchaseState.BILLING_IN_PROGRESS
            canPurchase -> PurchaseState.NOT_PURCHASED
            else -> PurchaseState.CANNOT_PURCHASE
        }

    private fun toUserBillingState(adState: AdState, purchaseState: PurchaseState, quality: Quality): UserBillingState =
        when {
            purchaseState == PurchaseState.PURCHASED -> UserBillingState.PURCHASED
            quality != Quality.High -> UserBillingState.EXEMPTED
            adState == AdState.VALIDATED -> UserBillingState.AD_WATCHED
            else -> UserBillingState.AD_REQUESTED
        }


    private fun initAdsOnConsentFlow(context: Context, consent: Flow<Boolean>, adsState: Flow<AdState>) : Flow<Unit> =
        combine(consent, adsState) { isConsenting, state ->
            if (!isConsenting || state != AdState.NOT_INITIALIZED) return@combine

            Log.i(TAG, "User consenting for ads, initialize ads SDK")
            adsDataSource.initialize(context)
        }

    private fun watchedAdInvalidatorFlow(userBillingState: Flow<UserBillingState>) : Flow<Any> =
        userBillingState.onEach { state ->
            if (resetAdJob != null || state != UserBillingState.AD_WATCHED) return@onEach

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
                .append("adsState=${adsState.dumpWithTimeout()}; ")
                .append("purchaseState=${purchaseState.dumpWithTimeout()}; ")
                .println()
            append(contentPrefix)
                .append("- proModeInfo=${proModeInfo.dumpWithTimeout()}; ")
                .println()
        }
    }
}

private val AD_WATCHED_STATE_DURATION = 1.hours
private const val TAG = "BillingRepository"