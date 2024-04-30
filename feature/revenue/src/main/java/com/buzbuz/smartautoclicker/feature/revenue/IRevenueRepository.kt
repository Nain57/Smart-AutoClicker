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
package com.buzbuz.smartautoclicker.feature.revenue

import android.app.Activity
import android.content.Context

import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration

import java.io.PrintWriter


interface IRevenueRepository : Dumpable {

    val isPrivacySettingRequired: Flow<Boolean>
    val userBillingState: StateFlow<UserBillingState>
    val isBillingFlowInProgress: Flow<Boolean>

    fun startUserConsentRequestUiFlowIfNeeded(activity: Activity)
    fun startPrivacySettingUiFlow(activity: Activity)

    fun loadAdIfNeeded(context: Context)
    fun startPaywallUiFlow(context: Context)

    fun startPurchaseUiFlow(context: Context)

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