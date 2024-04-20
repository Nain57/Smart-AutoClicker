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
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.feature.billing.IAdsRepository
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.data.ads.InterstitialAdsDataSource
import com.buzbuz.smartautoclicker.feature.billing.data.ads.RemoteInterstitialAd
import com.buzbuz.smartautoclicker.feature.billing.data.ads.UserConsentDataSource

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
) : IAdsRepository() {

    private val coroutineScopeMain: CoroutineScope =
        CoroutineScope(SupervisorJob() + mainDispatcher)

    private val pendingLoadRequest: MutableStateFlow<Boolean> =
        MutableStateFlow(false)

    init {
        // Once the user gave his consent, initialize the ads sdk
        userConsentDataSource.isUserConsentingForAds
            .onEach { isConsenting -> if (isConsenting) adsDataSource.initialize(context) }
            .launchIn(coroutineScopeMain)

        // Consume load request received before initialization
        combine(adsDataSource.remoteAd, pendingLoadRequest) { remoteAd, haveLoadRequest ->
            if (remoteAd == RemoteInterstitialAd.Initialized && haveLoadRequest) {
                pendingLoadRequest.emit(false)
                adsDataSource.loadAd(context)
            }
        }.launchIn(coroutineScopeMain)
    }

    override val isUserConsentingForAds: Flow<Boolean> = userConsentDataSource.isUserConsentingForAds
        .combine(billingRepository.isProModePurchased) { isConsenting, haveProMode ->
            !haveProMode && isConsenting
        }

    override val isPrivacyOptionsRequired: Flow<Boolean> = userConsentDataSource.isPrivacyOptionsRequired
        .combine(billingRepository.isProModePurchased) { required, haveProMode ->
            !haveProMode && required
        }

    override fun requestUserConsentIfNeeded(activity: Activity) {
        if (billingRepository.isPurchased()) return
        userConsentDataSource.requestUserConsent(activity)
    }

    override fun showPrivacyOptionsForm(activity: Activity) {
        userConsentDataSource.showPrivacyOptionsForm(activity)
    }

    override fun loadAd(context: Context) {
        if (!adsDataSource.isSdkInitialized()) {
            pendingLoadRequest.value = true
            return
        }

        adsDataSource.loadAd(context)
    }

    override fun showAd(activity: Activity) {
        adsDataSource.showAd(activity)
    }

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        super.dump(writer, prefix)
        writer.append(prefix.addDumpTabulationLvl())
            .append("- remoteInterstitialAd=").println("${adsDataSource.remoteAd.value}")
    }
}