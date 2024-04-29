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
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout
import com.buzbuz.smartautoclicker.core.common.quality.Quality
import com.buzbuz.smartautoclicker.core.common.quality.QualityManager
import com.buzbuz.smartautoclicker.feature.billing.AdState
import com.buzbuz.smartautoclicker.feature.billing.IAdsRepository
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.data.ads.InterstitialAdsDataSource
import com.buzbuz.smartautoclicker.feature.billing.data.ads.RemoteInterstitialAd
import com.buzbuz.smartautoclicker.feature.billing.data.ads.toAdState
import com.buzbuz.smartautoclicker.feature.billing.data.ads.UserConsentDataSource
import com.buzbuz.smartautoclicker.feature.billing.ui.AdsLoadingFragment
import com.buzbuz.smartautoclicker.feature.billing.ui.BillingActivity

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.hours

import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class AdsRepository @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(Main) mainDispatcher: CoroutineDispatcher,
    private val billingRepository: IBillingRepository,
    private val userConsentDataSource: UserConsentDataSource,
    private val adsDataSource: InterstitialAdsDataSource,
    qualityManager: QualityManager,
) : IAdsRepository() {

    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + mainDispatcher)

    /** Reset the ad shown state after a while */
    private var resetAdJob: Job? = null

    init {
        initAdsOnConsentFlow(context).launchIn(coroutineScopeMain)
        shownAdStateInvalidatorFlow().launchIn(coroutineScopeMain)
    }

    override val isUserConsentingForAds: Flow<Boolean> = userConsentDataSource.isUserConsentingForAds
        .combine(billingRepository.isProModePurchased) { isConsenting, haveProMode ->
            !haveProMode && isConsenting
        }

    override val isPrivacyOptionsRequired: Flow<Boolean> = userConsentDataSource.isPrivacyOptionsRequired
        .combine(billingRepository.isProModePurchased) { required, haveProMode ->
            !haveProMode && required
        }

    override val adsState: Flow<AdState> = adsDataSource.remoteAd
        .combine(qualityManager.quality) { remoteAd, quality ->
            if (quality != Quality.High) return@combine AdState.VALIDATED
            remoteAd.toAdState()
        }

    override fun requestUserConsentIfNeeded(activity: Activity) {
        if (billingRepository.isPurchased()) return
        userConsentDataSource.requestUserConsent(activity)
    }

    override fun showPrivacyOptionsForm(activity: Activity) {
        userConsentDataSource.showPrivacyOptionsForm(activity)
    }

    override fun loadAd(context: Context) {
        adsDataSource.loadAd(context)
    }

    override fun showAd(activity: Activity) {
        if (adsDataSource.remoteAd.value is RemoteInterstitialAd.Initialized)
            adsDataSource.loadAd(activity)

        // Keep the value now to get the new loading state from previous if
        val remoteAd = adsDataSource.remoteAd.value
        if (remoteAd is RemoteInterstitialAd.Loading || remoteAd is RemoteInterstitialAd.Error) {
            activity.startActivity(BillingActivity.getStartIntent(activity, AdsLoadingFragment.FRAGMENT_TAG))
            return
        }

        if (remoteAd is RemoteInterstitialAd.NotShown)
            adsDataSource.showAd(activity)
    }

    private fun initAdsOnConsentFlow(context: Context) : Flow<Unit> =
        combine(userConsentDataSource.isUserConsentingForAds, adsDataSource.remoteAd) { isConsenting, remoteAd ->
            if (!isConsenting || remoteAd != RemoteInterstitialAd.SdkNotInitialized) return@combine

            Log.i(TAG, "User consenting for ads, initialize ads SDK")
            adsDataSource.initialize(context)
        }

    private fun shownAdStateInvalidatorFlow() : Flow<Any> =
        adsDataSource.remoteAd.onEach { remoteAd ->
            if (remoteAd is RemoteInterstitialAd.Shown) {
                resetAdJob?.cancel()
                resetAdJob = coroutineScopeMain.launch {
                    delay(1.hours)
                    adsDataSource.reset()
                }
            }
        }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        super.dump(writer, prefix)
        writer.append(prefix.addDumpTabulationLvl())
            .append("- remoteInterstitialAd=${adsDataSource.remoteAd.value}; ")
            .append("adsState=${adsState.dumpWithTimeout()}; ")
            .println()
    }
}

private const val TAG = "AdsRepository"