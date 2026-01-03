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
package com.buzbuz.smartautoclicker.feature.revenue.data.ads.sdk

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.annotation.MainThread

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.feature.revenue.BuildConfig

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class GoogleAdsSdk @Inject constructor(
    @Dispatcher(Main) dispatcherMain: CoroutineDispatcher,
) : IAdsSdk {

    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatcherMain)

    /** How long before the data source tries to reconnect to Google play. */
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
    private var reconnectJob: Job? = null

    private var interstitialAd: InterstitialAd? = null

    @MainThread
    override fun initializeSdk(context: Context, onComplete: () -> Unit) {
        try {
            MobileAds.initialize(context) { onComplete() }
        } catch (ex: Exception) {
            Log.e(LOG_TAG, "Error while initializing Google Ads SDK, retrying...", ex)
            retryInitWithExponentialBackoff(context, onComplete)
        }
    }

    @MainThread
    override fun setTestDevices(deviceIds: List<String>) {
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(deviceIds).build()
        )
    }

    @MainThread
    override fun loadInterstitialAd(
        context: Context,
        onLoaded: () -> Unit,
        onError: (code: Int, message: String) -> Unit,
    ) = InterstitialAd.load(
        context,
        BuildConfig.ADS_UNIT_ID,
        buildInterstitialAdRequest(),
        newAdLoadCallback(onLoaded, onError),
    )

    @MainThread
    override fun showInterstitialAd(
        activity: Activity,
        onShow: () -> Unit,
        onDismiss: (impression: Boolean) -> Unit,
        onError: (code: Int, message: String) -> Unit,
    ) {
        val ad = interstitialAd ?: return

        ad.fullScreenContentCallback = newAdShowCallback(onShow, onDismiss, onError)
        ad.show(activity)
        interstitialAd = null
    }

    private fun buildInterstitialAdRequest(): AdRequest =
        AdRequest.Builder().build()

    private fun newAdLoadCallback(onLoaded: () -> Unit, onError: (code: Int, message: String) -> Unit) =
        object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                onLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError): Unit =
                onError(adError.code, adError.message)
        }

    private fun newAdShowCallback(
        onShow: () -> Unit,
        onDismiss: (impression: Boolean) -> Unit,
        onError: (code: Int, message: String) -> Unit,
    ) = object : FullScreenContentCallback() {
        var impression = false
        override fun onAdImpression() { impression = true }
        override fun onAdShowedFullScreenContent(): Unit = onShow()
        override fun onAdDismissedFullScreenContent(): Unit = onDismiss(impression)
        override fun onAdFailedToShowFullScreenContent(adError: AdError): Unit = onError(adError.code, adError.message)
    }

    /**
     * Retries the Google Ads SDK with exponential backoff, maxing out at the time
     * specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS.
     */
     private fun retryInitWithExponentialBackoff(context: Context, onComplete: () -> Unit) {
        if (reconnectJob != null) {
            Log.e(LOG_TAG, "Reconnection job is already running")
            return
        }

        reconnectJob = coroutineScopeMain.launch {
            delay(reconnectMilliseconds)
            reconnectMilliseconds = min(reconnectMilliseconds * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS)

            reconnectJob = null
            initializeSdk(context, onComplete)
        }
    }
}

private const val RECONNECT_TIMER_START_MILLISECONDS = 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes
private const val LOG_TAG = "GoogleAdsSdk"