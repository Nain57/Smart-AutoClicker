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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.interstitial.InterstitialAd

internal fun InterstitialAd.show(
    activity: Activity,
    onShow: () -> Unit,
    onDismiss: (impression: Boolean) -> Unit,
    onError: (AdError) -> Unit,
) {
    var impression = false
    fullScreenContentCallback = object : FullScreenContentCallback() {
        override fun onAdImpression() { impression = true }
        override fun onAdShowedFullScreenContent(): Unit = onShow()
        override fun onAdDismissedFullScreenContent(): Unit = onDismiss(impression)
        override fun onAdFailedToShowFullScreenContent(adError: AdError): Unit = onError(adError)
    }

    show(activity)
}
