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

import com.buzbuz.smartautoclicker.feature.billing.AdState

internal sealed class RemoteInterstitialAd {

    data object SdkNotInitialized : RemoteInterstitialAd()
    data object Initialized : RemoteInterstitialAd()
    data object Loading : RemoteInterstitialAd()
    data object NotShown : RemoteInterstitialAd()
    data object Showing : RemoteInterstitialAd()
    data object Shown : RemoteInterstitialAd()

    sealed class Error : RemoteInterstitialAd() {
        abstract val code: Int?
        abstract val message: String?

        data class LoadingError(override val code: Int, override val message: String) : Error()
        data class ShowError(override val code: Int, override val message: String) : Error()
        data object NoImpressionError : Error() {
            override val code = null
            override val message = null
        }
    }
}

internal fun RemoteInterstitialAd.toAdState(): AdState = when (this) {
    RemoteInterstitialAd.SdkNotInitialized,
    RemoteInterstitialAd.Initialized -> AdState.REQUESTED

    RemoteInterstitialAd.Loading -> AdState.LOADING

    is RemoteInterstitialAd.NotShown -> AdState.READY

    RemoteInterstitialAd.Showing -> AdState.SHOWING

    is RemoteInterstitialAd.Shown -> AdState.VALIDATED

    is RemoteInterstitialAd.Error.LoadingError,
    is RemoteInterstitialAd.Error.ShowError,
    RemoteInterstitialAd.Error.NoImpressionError -> AdState.ERROR
}