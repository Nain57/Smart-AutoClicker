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
package com.buzbuz.smartautoclicker.feature.revenue.navigation

import android.app.Activity
import android.content.Context
import com.buzbuz.smartautoclicker.core.common.navigation.RevenueNavigator
import com.buzbuz.smartautoclicker.core.common.revenue.domain.IRevenueRepository
import com.buzbuz.smartautoclicker.feature.revenue.ui.BillingActivity
import com.buzbuz.smartautoclicker.feature.revenue.ui.paywall.PaywallFragment
import com.buzbuz.smartautoclicker.feature.revenue.ui.purchase.PurchaseProModeFragment
import javax.inject.Inject

internal class RevenueNavigatorImpl @Inject constructor(
    private val revenueRepository: IRevenueRepository,
): RevenueNavigator {

    override fun startUserConsentRequestUiFlowIfNeeded(activity: Activity) {
        revenueRepository.startUserConsentRequestUiFlowIfNeeded(activity)
    }

    override fun startPrivacySettingUiFlow(activity: Activity) {
        revenueRepository.startPrivacySettingUiFlow(activity)
    }

    override fun startPaywallActivity(context: Context) {
        context.startActivity(
            BillingActivity.getStartIntent(
                context = context,
                billingFragment = PaywallFragment.FRAGMENT_TAG,
            )
        )
    }

    override fun startPurchaseActivity(context: Context) {
        context.startActivity(
            BillingActivity.getStartIntent(
                context = context,
                billingFragment = PurchaseProModeFragment.FRAGMENT_TAG,
            )
        )
    }
}