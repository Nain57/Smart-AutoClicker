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

import android.content.Context
import androidx.annotation.MainThread

import com.buzbuz.smartautoclicker.feature.billing.BuildConfig

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

typealias AdsUnitId = String

@MainThread // Required from ad API
internal fun AdsUnitId.load(
    context: Context,
    onLoad: (InterstitialAd) -> Unit,
    onError: (LoadAdError) -> Unit,
) {
    InterstitialAd.load(
        context,
        this,
        buildAdRequest(),
        object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd): Unit = onLoad(interstitialAd)
            override fun onAdFailedToLoad(adError: LoadAdError): Unit = onError(adError)
        },
    )
}

private fun buildAdRequest(): AdRequest =
    AdRequest.Builder().build()

private const val INTERSTITIAL_AD_TEST_ID: AdsUnitId = "ca-app-pub-3940256099942544/1033173712"
private const val INTERSTITIAL_VIDEO_AD_TEST_ID: AdsUnitId = "ca-app-pub-3940256099942544/8691691433"

internal val adsUnitId: AdsUnitId =
    if (BuildConfig.DEBUG) INTERSTITIAL_VIDEO_AD_TEST_ID
    else BuildConfig.ADS_APPLICATION_ID
