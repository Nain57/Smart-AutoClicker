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
import com.buzbuz.smartautoclicker.feature.billing.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class InterstitialAdsDataSource @Inject constructor() {

    private val adsId: String =
        if (BuildConfig.DEBUG) INTERSTITIAL_AD_TEST_ID
        else BuildConfig.ADS_APPLICATION_ID

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

        Log.d(TAG, "Load interstitial ad with id $adsId")

        _remoteAd.value = RemoteInterstitialAd.Loading
        loadInterstitialAd(context) { interstitialAd ->
            _remoteAd.value =
                if (interstitialAd != null) RemoteInterstitialAd.Loaded.NotShown(interstitialAd)
                else RemoteInterstitialAd.Error
        }
    }

    fun showAd(activity: Activity) {
        val remoteAd = _remoteAd.value
        if (remoteAd !is RemoteInterstitialAd.Loaded) return

        Log.d(TAG, "Show interstitial ad with id $adsId")
        remoteAd.ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.d(TAG, "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed fullscreen content.")
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show fullscreen content.")
            }

            override fun onAdImpression() {
                Log.d(TAG, "Ad recorded an impression.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
            }
        }

        remoteAd.ad.show(activity)
    }

    private fun buildAdRequest(): AdRequest =
        AdRequest.Builder().build()

    private fun loadInterstitialAd(context: Context, onLoad: (InterstitialAd?) -> Unit): Unit =
        InterstitialAd.load(
            context, adsId, buildAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.w(TAG, "onAdFailedToLoad: $adError")
                    onLoad(null)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "onAdLoaded")
                    onLoad(interstitialAd)
                }
            },
        )
}

private const val INTERSTITIAL_AD_TEST_ID = "ca-app-pub-3940256099942544/1033173712"
private const val TAG = "InterstitialAdsDataSource"