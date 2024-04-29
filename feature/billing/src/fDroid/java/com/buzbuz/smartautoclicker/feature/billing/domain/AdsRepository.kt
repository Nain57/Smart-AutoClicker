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
import com.buzbuz.smartautoclicker.feature.billing.IAdsRepository
import com.buzbuz.smartautoclicker.feature.billing.RemoteInterstitialAd
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

internal class AdsRepository @Inject constructor() : IAdsRepository() {

    override val isUserConsentingForAds: Flow<Boolean> = flowOf(false)
    override val isPrivacyOptionsRequired: Flow<Boolean> = flowOf(false)
    override val adsState: Flow<RemoteInterstitialAd> = flowOf(RemoteInterstitialAd.SdkNotInitialized)

    override fun requestUserConsentIfNeeded(activity: Activity) = Unit
    override fun showPrivacyOptionsForm(activity: Activity) = Unit

    override fun loadAd(context: Context) = Unit
    override fun showAd(activity: Activity) = Unit
}