/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.billing.ui

import android.app.Activity
import android.content.Context

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.R

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** View model for the [ProModeBillingDialogFragment]. */
@HiltViewModel
internal class ProModeBillingViewModel @Inject constructor(
    @ApplicationContext appContext: Context,
    private val billingRepository: IBillingRepository,
) : ViewModel(), LifecycleEventObserver {

    private val proModeFeature = MutableStateFlow<ProModeAdvantage?>(null)

    val dialogState: Flow<DialogState> = combine(
        billingRepository.isProModePurchased,
        proModeFeature.filterNotNull(),
        billingRepository.proModePrice,
    ) { isPurchased, reason, price ->
        if (isPurchased) DialogState.Purchased
        else DialogState.NotPurchased(
            reason.toDisplayString(appContext),
            price,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(3000),
        DialogState.Connecting,
    )

    fun setBillingReason(reason: ProModeAdvantage?) {
        proModeFeature.value = reason
    }

    fun launchPlayStoreBillingFlow(activity: Activity) {
        billingRepository.launchPlayStoreBillingFlow(activity)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        billingRepository.setBillingActivityState(event != Lifecycle.Event.ON_DESTROY)
    }
}

internal sealed class DialogState {
    internal data class NotPurchased(
        val billingReasonText: String?,
        val acceptButtonText: String,
    ): DialogState()

    internal data object Purchased : DialogState()

    internal data object Connecting : DialogState()
}


private fun ProModeAdvantage?.toDisplayString(context: Context): String? =
    when (this) {
        is ProModeAdvantage.Feature -> this.toDisplayString(context)
        is ProModeAdvantage.Limitation -> this.toDisplayString(context)
        null -> null
        else -> throw UnsupportedOperationException("Can't get the string res value for this ProModeAdvantage")
    }

private fun ProModeAdvantage.Feature.toDisplayString(context: Context): String =
    context.getString(
        when (this) {
            ProModeAdvantage.Feature.ACTION_TYPE_INTENT -> R.string.billing_reason_action_type_intent
            ProModeAdvantage.Feature.ACTION_TYPE_TOGGLE_EVENT -> R.string.billing_reason_action_type_toggle_event
            ProModeAdvantage.Feature.BACKUP_EXPORT -> R.string.billing_reason_backup_export
            ProModeAdvantage.Feature.BACKUP_IMPORT -> R.string.billing_reason_backup_import
            ProModeAdvantage.Feature.EVENT_STATE -> R.string.billing_reason_event_state
            ProModeAdvantage.Feature.SCENARIO_ANTI_DETECTION -> R.string.billing_reason_scenario_anti_detection
            ProModeAdvantage.Feature.SCENARIO_DETECTION_QUALITY -> R.string.billing_reason_scenario_detection_quality
            ProModeAdvantage.Feature.SCENARIO_END_CONDITIONS -> R.string.billing_reason_scenario_end_conditions
        }
    )


private fun ProModeAdvantage.Limitation.toDisplayString(context: Context): String =
    context.getString(
        when (this) {
            ProModeAdvantage.Limitation.ACTION_COUNT_LIMIT -> R.string.billing_reason_action_count
            ProModeAdvantage.Limitation.CONDITION_COUNT_LIMIT -> R.string.billing_reason_condition_count
            ProModeAdvantage.Limitation.DETECTION_DURATION_MINUTES_LIMIT -> R.string.billing_reason_detection_duration_limit
            ProModeAdvantage.Limitation.EVENT_COUNT_LIMIT -> R.string.billing_reason_event_count
            ProModeAdvantage.Limitation.SMART_SCENARIO_COUNT_LIMIT -> R.string.billing_reason_scenario_count
        },
        limit,
    )