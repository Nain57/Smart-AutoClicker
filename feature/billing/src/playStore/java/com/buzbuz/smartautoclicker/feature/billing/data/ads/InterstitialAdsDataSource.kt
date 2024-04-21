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
package com.buzbuz.smartautoclicker.feature.billing.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class InterstitialAdsDataSource @Inject constructor(
    @Dispatcher(Main) mainDispatcher: CoroutineDispatcher,
) {
    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + mainDispatcher)

    /** The number of automatic retries for the loading of an ad. */
    private var loadRetries: Int = 0

    private val _remoteAd: MutableStateFlow<RemoteInterstitialAd> =
        MutableStateFlow(RemoteInterstitialAd.SdkNotInitialized)
    val remoteAd: StateFlow<RemoteInterstitialAd> = _remoteAd

    fun isSdkInitialized(): Boolean =
        _remoteAd.value != RemoteInterstitialAd.SdkNotInitialized

    fun initialize(context: Context) {
        if (_remoteAd.value != RemoteInterstitialAd.SdkNotInitialized) return

        Log.d(TAG, "Initialize MobileAds")

        MobileAds.initialize(context)
        _remoteAd.value = RemoteInterstitialAd.Initialized
    }

    fun loadAd(context: Context) {
        if (_remoteAd.value == RemoteInterstitialAd.SdkNotInitialized) return

        Log.d(TAG, "Load interstitial ad with id $adsUnitId")

        _remoteAd.value = RemoteInterstitialAd.Loading
        adsUnitId.load(
            context = context,
            onLoad = ::onAdLoaded,
            onError = { error -> onAdLoadFailed(context, error) },
        )
    }

    fun showAd(activity: Activity) {
        val remoteAd = _remoteAd.value
        if (remoteAd !is RemoteInterstitialAd.NotShown) return

        Log.d(TAG, "Showing interstitial ad")

        remoteAd.ad.show(
            activity = activity,
            onShow = ::onAdShown,
            onDismiss = ::onAdDismissed,
            onError = ::onAdShowError,
        )
    }

    private fun onAdLoaded(ad: InterstitialAd) {
        Log.d(TAG, "onAdLoaded")
        loadRetries = 0
        _remoteAd.value = RemoteInterstitialAd.NotShown(ad)
    }

    private fun onAdLoadFailed(context: Context, error: LoadAdError) {
        Log.w(TAG, "onAdFailedToLoad: $error, retry=$loadRetries/$MAX_LOADING_RETRIES")

        if (loadRetries > MAX_LOADING_RETRIES) {
            _remoteAd.value = RemoteInterstitialAd.Error.LoadingError(error)
            loadRetries = 0
            Log.e(TAG, "Can't load ad")
            return
        }

        loadRetries++
        coroutineScopeMain.launch {
            delay(RETRY_INCREMENTAL_DELAY_MS * loadRetries)
            loadAd(context)
        }
    }

    private fun onAdShown() {
        Log.d(TAG, "onAdShown")
        _remoteAd.value = RemoteInterstitialAd.Showing
    }

    private fun onAdDismissed(impression: Boolean) {
        Log.d(TAG, "onAdDismissed, impression=$impression")

        _remoteAd.value =
            if (impression) RemoteInterstitialAd.Shown()
            else RemoteInterstitialAd.Error.ShowError()
    }

    private fun onAdShowError(error: AdError) {
        Log.w(TAG, "onAdShowError: $error")
        _remoteAd.value = RemoteInterstitialAd.Error.ShowError(error)
    }
}

private const val RETRY_INCREMENTAL_DELAY_MS = 500L
private const val MAX_LOADING_RETRIES = 5
private const val TAG = "InterstitialAdsDataSource"