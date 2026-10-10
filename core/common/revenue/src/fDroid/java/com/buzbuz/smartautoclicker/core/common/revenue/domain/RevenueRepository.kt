/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.common.revenue.domain

import android.app.Activity
import android.content.Context
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.AdState
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.ProModeInfo
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.PurchaseState
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.UserBillingState
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.UserConsentState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import kotlin.time.Duration

internal class RevenueRepository @Inject constructor() : IRevenueRepository {

    override val userConsentState: Flow<UserConsentState> = flowOf(UserConsentState.CANNOT_REQUEST_ADS)
    override val isPrivacySettingRequired: Flow<Boolean> = flowOf(false)
    override val userBillingState: StateFlow<UserBillingState> = MutableStateFlow(UserBillingState.PURCHASED)
    override val proModeInfo: Flow<ProModeInfo?> = flowOf(null)
    override val adsState: StateFlow<AdState> = MutableStateFlow(AdState.NOT_INITIALIZED)
    override val purchaseState: StateFlow<PurchaseState> = MutableStateFlow(PurchaseState.PURCHASED)
    override val trialAvailable: Flow<Boolean> = flowOf(false)
    override val paywallFinished: SharedFlow<Unit> = MutableSharedFlow()

    override fun startUserConsentRequestUiFlowIfNeeded(activity: Activity) = Unit
    override fun startPrivacySettingUiFlow(activity: Activity) = Unit
    override fun startPlayStoreBillingUiFlow(activity: Activity) = Unit
    override fun notifyPaywallFinished() = Unit
    override fun refreshPurchases() = Unit
    override fun loadAdIfNeeded(context: Context) = Unit
    override fun showAd(activity: Activity) = Unit
    override fun requestTrial() = Unit
    override fun consumeTrial(): Duration? = null
}
