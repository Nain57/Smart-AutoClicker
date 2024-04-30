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
package com.buzbuz.smartautoclicker.feature.revenue.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels

import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.feature.revenue.R
import com.buzbuz.smartautoclicker.feature.revenue.ui.paywall.PaywallFragment
import com.buzbuz.smartautoclicker.feature.revenue.ui.purchase.PurchaseProModeFragment

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class BillingActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_FRAGMENT_TAG =
            "com.buzbuz.smartautoclicker.feature.revenue.ui.EXTRA_FRAGMENT_TAG"

        /**
         * Get the intent for starting this activity.
         *
         * @param context the Android context.
         * @param billingFragment the tag of the fragment to be displayed.
         *
         * @return the intent, ready to be sent.
         */
        fun getStartIntent(context: Context, billingFragment: String): Intent =
            Intent(context, BillingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_FRAGMENT_TAG, billingFragment)
    }

    private val viewModel: BillingActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pro_mode)

        when (val tag = intent?.getStringExtra(EXTRA_FRAGMENT_TAG)) {
            PaywallFragment.FRAGMENT_TAG -> PaywallFragment()
                .show(supportFragmentManager, PaywallFragment.FRAGMENT_TAG)

            PurchaseProModeFragment.FRAGMENT_TAG -> PurchaseProModeFragment()
                .show(supportFragmentManager, PurchaseProModeFragment.FRAGMENT_TAG)

            else -> {
                Log.e(TAG, "Invalid fragment tag $tag")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.setBillingActivityDestroyed()
    }
}

private const val TAG = "BillingActivity"
