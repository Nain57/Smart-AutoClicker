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
package com.buzbuz.smartautoclicker.feature.revenue.ui.purchase

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.feature.revenue.domain.InternalRevenueRepository
import com.buzbuz.smartautoclicker.feature.revenue.domain.model.PurchaseState

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

import javax.inject.Inject

@HiltViewModel
internal class PurchaseProModeViewModel @Inject constructor(
    private val revenueRepository: InternalRevenueRepository,
) : ViewModel() {

    val dialogState: Flow<PurchaseDialogState> =
        combine(revenueRepository.purchaseState, revenueRepository.proModeInfo) { state, info ->
            when {
                state == PurchaseState.PURCHASED -> PurchaseDialogState.Purchased
                state == PurchaseState.CANNOT_PURCHASE -> PurchaseDialogState.Error
                info == null -> PurchaseDialogState.Loading
                else -> PurchaseDialogState.Loaded(info.price)
            }
        }

    fun launchPlayStoreBillingFlow(activity: Activity) {
        revenueRepository.startPlayStoreBillingUiFlow(activity)
    }

    fun getGitHubWebPageIntent(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nain57/Smart-AutoClicker"))
}

internal sealed class PurchaseDialogState {
    data object Loading : PurchaseDialogState()
    data object Purchased : PurchaseDialogState()
    data object Error : PurchaseDialogState()
    data class Loaded(val price: String) : PurchaseDialogState()
}