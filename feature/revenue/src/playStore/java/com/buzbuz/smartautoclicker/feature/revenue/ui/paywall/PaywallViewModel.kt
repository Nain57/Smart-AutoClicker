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
package com.buzbuz.smartautoclicker.feature.revenue.ui.paywall

import android.app.Activity
import android.content.Context

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.ui.bindings.LoadableButtonState
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.AdState
import com.buzbuz.smartautoclicker.feature.revenue.R
import com.buzbuz.smartautoclicker.feature.revenue.domain.InternalRevenueRepository
import com.buzbuz.smartautoclicker.feature.revenue.domain.TRIAL_SESSION_DURATION_DURATION
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.ProModeInfo
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.PurchaseState

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
internal class AdsLoadingViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val revenueRepository: InternalRevenueRepository,
) : ViewModel() {

    val dialogState: Flow<DialogState> = combine(
        revenueRepository.purchaseState,
        revenueRepository.adsState,
        revenueRepository.proModeInfo,
    ) { purchaseState, adsState, info ->
        when {
            purchaseState == PurchaseState.PURCHASED -> DialogState.Purchased
            adsState == AdState.VALIDATED -> DialogState.AdWatched
            else -> DialogState.NotPurchased(
                trialDurationMinutes = TRIAL_SESSION_DURATION_DURATION.inWholeMinutes.toInt(),
                adButtonState = adsState.toAdButtonState(appContext),
                purchaseButtonState = getPurchaseButtonState(appContext, purchaseState, info),
            )
        }
    }

    fun launchPlayStoreBillingFlow(activity: Activity) {
        revenueRepository.startPlayStoreBillingUiFlow(activity)
    }

    fun showAd(activity: Activity) {
        revenueRepository.showAd(activity)
    }

    fun requestTrial() {
        revenueRepository.requestTrial()
    }
}

internal sealed class DialogState {

    internal data class NotPurchased(
        val trialDurationMinutes: Int,
        val adButtonState: LoadableButtonState,
        val purchaseButtonState: LoadableButtonState,
    ): DialogState()

    internal data object Purchased : DialogState()

    internal data object AdWatched : DialogState()
}


private fun AdState.toAdButtonState(context: Context): LoadableButtonState = when (this) {
    AdState.INITIALIZED,
    AdState.LOADING -> LoadableButtonState.Loading

    AdState.READY -> LoadableButtonState.Loaded.Enabled(
        text = context.getString(R.string.button_text_watch_ad)
    )

    AdState.SHOWING,
    AdState.VALIDATED -> LoadableButtonState.Loaded.Disabled(
        text = context.getString(R.string.button_text_watch_ad)
    )

    AdState.NOT_INITIALIZED,
    AdState.ERROR -> LoadableButtonState.Loaded.Disabled(
        text = context.getString(R.string.button_text_watch_ad_error)
    )
}

private fun getPurchaseButtonState(context: Context, purchaseState: PurchaseState, info: ProModeInfo?): LoadableButtonState {
    val price = info?.price
    if (price.isNullOrEmpty() || purchaseState == PurchaseState.BILLING_IN_PROGRESS)
        return LoadableButtonState.Loading

    if (purchaseState == PurchaseState.CANNOT_PURCHASE)
        return LoadableButtonState.Loaded.Disabled(
            text = context.getString(R.string.button_text_buy_pro_error)
        )

    return LoadableButtonState.Loaded.Enabled(
        text = context.getString(R.string.button_text_buy_pro, price)
    )
}