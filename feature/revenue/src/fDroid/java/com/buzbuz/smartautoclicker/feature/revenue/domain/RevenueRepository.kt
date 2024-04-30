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
package com.buzbuz.smartautoclicker.feature.revenue.domain

import android.app.Activity
import android.content.Context

import com.buzbuz.smartautoclicker.feature.revenue.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.revenue.UserBillingState

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

import javax.inject.Inject
import kotlin.time.Duration


internal class RevenueRepository @Inject constructor(): IRevenueRepository {

    override val isPrivacySettingRequired: Flow<Boolean> = flowOf(false)
    override val userBillingState: StateFlow<UserBillingState> = MutableStateFlow(UserBillingState.PURCHASED)
    override val isBillingFlowInProgress: Flow<Boolean> = flowOf(false)

    override fun startUserConsentRequestUiFlowIfNeeded(activity: Activity) = Unit
    override fun startPrivacySettingUiFlow(activity: Activity) = Unit
    override fun loadAdIfNeeded(context: Context) = Unit
    override fun startPaywallUiFlow(context: Context) = Unit
    override fun startPurchaseUiFlow(context: Context) = Unit
    override fun consumeTrial(): Duration? = null

}