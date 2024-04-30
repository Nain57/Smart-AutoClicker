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
import androidx.annotation.MainThread
import com.google.android.gms.ads.AdError

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAdsSdk @Inject constructor() : IAdsSdk {

    private var interstitialAd: InterstitialAd? = null

    @MainThread
    override fun initializeSdk(context: Context, onComplete: () -> Unit) {
        MobileAds.initialize(context) { onComplete() }
    }

    @MainThread
    override fun loadInterstitialAd(
        context: Context,
        onLoaded: () -> Unit,
        onError: (code: Int, message: String) -> Unit,
    ) = InterstitialAd.load(
        context,
        adsUnitId,
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
}