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
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.AdState
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.ProModeInfo
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.PurchaseState
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.UserBillingState
import com.buzbuz.smartautoclicker.core.common.revenue.domain.model.UserConsentState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.PrintWriter
import kotlin.time.Duration

interface IRevenueRepository : Dumpable {

    val userConsentState: Flow<UserConsentState>
    val isPrivacySettingRequired: Flow<Boolean>

    val userBillingState: StateFlow<UserBillingState>

    val proModeInfo: Flow<ProModeInfo?>
    val adsState: StateFlow<AdState>
    val purchaseState: StateFlow<PurchaseState>
    val trialAvailable: Flow<Boolean>

    val paywallFinished: SharedFlow<Unit>

    fun startUserConsentRequestUiFlowIfNeeded(activity: Activity)
    fun startPrivacySettingUiFlow(activity: Activity)


    fun startPlayStoreBillingUiFlow(activity: Activity)
    fun notifyPaywallFinished()

    fun refreshPurchases()

    fun loadAdIfNeeded(context: Context)
    fun showAd(activity: Activity)
    fun requestTrial()
    fun consumeTrial(): Duration?

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* RevenueRepository:")
            append(contentPrefix)
                .append("- userBillingState=${userBillingState.dumpWithTimeout()}; ")
                .append("isPrivacySettingRequired=${isPrivacySettingRequired.dumpWithTimeout()}; ")
                .println()
        }
    }
}