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

import android.content.Context
import android.content.Intent
import android.os.Bundle

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.feature.billing.R

class ProModeBillingActivity : AppCompatActivity() {

    companion object {

        /** Intent extra key for the billing reason. */
        private const val EXTRA_BILLING_REASON =
            "com.buzbuz.smartautoclicker.feature.billing.ui.EXTRA_BILLING_REASON"
        /**
         * Intent extra key for limitations. If true, the billing reason is of type [ProModeAdvantage.Limitation].
         * If not, it is of type [ProModeAdvantage.Feature].
         */
        private const val EXTRA_IS_LIMITATION =
            "com.buzbuz.smartautoclicker.feature.billing.ui.EXTRA_IS_LIMITATION"

        /**
         * Get the intent for starting this activity.
         *
         * @param context the Android context.
         * @param advantage the reason for displaying the billing activity.
         *
         * @return the intent, ready to be sent.
         */
        internal fun getStartIntent(context: Context, advantage: ProModeAdvantage): Intent =
            Intent(context, ProModeBillingActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_BILLING_REASON, advantage.toString())
                .putExtra(EXTRA_IS_LIMITATION, advantage is ProModeAdvantage.Limitation)

        /** Get the billing reason from the intent extra. */
        private fun Intent.getBillingReasonExtra(): ProModeAdvantage? {
            val reasonString = getStringExtra(EXTRA_BILLING_REASON)
            if (reasonString == null || !hasExtra(EXTRA_IS_LIMITATION)) return null

            return if (getBooleanExtra(EXTRA_IS_LIMITATION, false)) ProModeAdvantage.Limitation.valueOf(reasonString)
                else ProModeAdvantage.Feature.valueOf(reasonString)
        }
    }

    /** The view model shared between this activity and the dialog fragment. */
    private val billingViewModel: ProModeBillingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pro_mode)

        // Set the billing reason in the view model, it will be propagated to the dialog.
        billingViewModel.setBillingReason(intent.getBillingReasonExtra())

        lifecycle.addObserver(billingViewModel)

        // Creates and show the dialog.
        ProModeBillingDialogFragment()
            .show(supportFragmentManager, ProModeBillingDialogFragment.FRAGMENT_TAG)
    }
}
