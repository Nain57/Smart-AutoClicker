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
import android.util.Log
import androidx.annotation.VisibleForTesting

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.feature.revenue.data.ads.sdk.IAdsSdk

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class InterstitialAdsDataSource @Inject constructor(
    @ApplicationContext context: Context,
    @Dispatcher(Main) mainDispatcher: CoroutineDispatcher,
    private val adsSdk: IAdsSdk,
) {
    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + mainDispatcher)

    /** The number of automatic retries for the loading of an ad. */
    private var loadRetries: Int = 0
    /**
     * If a call to [loadAd] is made before the sdk is initialized, we register it and execute it as soon as the sdk
     * initialization is complete. Executed on the main thread due to sdk requirement.
     */
    private val pendingLoadRequest: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    private val _remoteAdState: MutableStateFlow<RemoteAdState> =
        MutableStateFlow(RemoteAdState.SdkNotInitialized)
    val remoteAdState: StateFlow<RemoteAdState> = _remoteAdState

    init {
        loadAdRequestConsumerFlow(context, _remoteAdState, pendingLoadRequest)
            .launchIn(coroutineScopeMain)
    }

    fun initialize(context: Context) {
        if (_remoteAdState.value != RemoteAdState.SdkNotInitialized) return

        Log.i(TAG, "Initialize MobileAds")

        adsSdk.initializeSdk(context) {
            coroutineScopeMain.launch {
                _remoteAdState.emit(RemoteAdState.Initialized)
            }
        }
    }

    fun loadAd(context: Context) {
        val adState = _remoteAdState.value
        if (adState == RemoteAdState.SdkNotInitialized) {
            Log.i(TAG, "Load ad request delayed, SDK is not initialized")
            pendingLoadRequest.value = true
            return
        }

        if (adState != RemoteAdState.Initialized) return

        Log.i(TAG, "Load interstitial ad")
        coroutineScopeMain.launch {
            _remoteAdState.emit(RemoteAdState.Loading)
            adsSdk.loadInterstitialAd(
                context = context,
                onLoaded = ::onAdLoaded,
                onError = { code, message -> onAdLoadFailed(context, code, message) },
            )
        }
    }


    fun showAd(activity: Activity) {
        val remoteAd = _remoteAdState.value
        if (remoteAd !is RemoteAdState.NotShown) {
            Log.w(TAG, "Can't show ad, loading is not completed")
            return
        }

        Log.i(TAG, "Show interstitial ad")

        adsSdk.showInterstitialAd(
            activity = activity,
            onShow = ::onAdShown,
            onDismiss = ::onAdDismissed,
            onError = ::onAdShowError,
        )
    }

    fun reset() {
        if (_remoteAdState.value == RemoteAdState.Initialized
            || _remoteAdState.value == RemoteAdState.SdkNotInitialized
        ) return

        Log.i(TAG, "Reset ad state")
        coroutineScopeMain.launch {
            _remoteAdState.emit(RemoteAdState.Initialized)
        }
    }

    private fun onAdLoaded() {
        Log.i(TAG, "onAdLoaded")
        coroutineScopeMain.launch {
            loadRetries = 0
            _remoteAdState.emit(RemoteAdState.NotShown)
        }
    }

    private fun onAdLoadFailed(context: Context, errorCode: Int, errorMessage: String) {
        Log.w(TAG, "onAdFailedToLoad: retry=$loadRetries/$MAX_LOADING_RETRIES; $errorCode:$errorMessage")

        coroutineScopeMain.launch {
            loadRetries++

            if (loadRetries >= MAX_LOADING_RETRIES) {
                _remoteAdState.emit(RemoteAdState.Error.LoadingError(errorCode, errorMessage))
                loadRetries = 0
                Log.e(TAG, "Can't load ad")
                return@launch
            }

            delay(RETRY_INCREMENTAL_DELAY_MS * loadRetries)

            _remoteAdState.value = RemoteAdState.Initialized
            loadAd(context)
        }
    }

    private fun onAdShown() {
        Log.i(TAG, "onAdShown")

        coroutineScopeMain.launch {
            _remoteAdState.emit(RemoteAdState.Showing)
        }
    }

    private fun onAdDismissed(impression: Boolean) {
        Log.i(TAG, "onAdDismissed, impression=$impression")

        coroutineScopeMain.launch {
            _remoteAdState.emit(
                if (impression) RemoteAdState.Shown
                else RemoteAdState.Error.NoImpressionError
            )
        }
    }

    private fun onAdShowError(errorCode: Int, errorMessage: String) {
        Log.w(TAG, "onAdShowError: $errorCode:$errorMessage")

        coroutineScopeMain.launch {
            _remoteAdState.emit(RemoteAdState.Error.ShowError(errorCode, errorMessage))
        }
    }

    private fun loadAdRequestConsumerFlow(
        context: Context,
        adState: Flow<RemoteAdState>,
        isPending: MutableStateFlow<Boolean>,
    ) : Flow<Unit> =
        combine(adState, isPending) { remoteAd, haveLoadRequest ->
            if (remoteAd != RemoteAdState.Initialized || !haveLoadRequest) return@combine

            Log.i(TAG, "Ads SDK is now initialized, consuming pending ad load request")

            isPending.emit(false)
            loadAd(context)
        }
}

@VisibleForTesting internal const val MAX_LOADING_RETRIES = 5
private const val RETRY_INCREMENTAL_DELAY_MS = 500L
private const val TAG = "InterstitialAdsDataSource"