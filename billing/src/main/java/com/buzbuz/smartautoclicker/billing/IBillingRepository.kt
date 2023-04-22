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
package com.buzbuz.smartautoclicker.billing

import android.app.Activity
import android.content.Context

import kotlinx.coroutines.flow.Flow

interface IBillingRepository {

    companion object {

        /** Singleton preventing multiple instances of the repository at the same time. */
        @Volatile
        private var INSTANCE: BillingRepository? = null

        /**
         * Get the repository singleton, or instantiates it if it wasn't yet.
         *
         * @param context the Android context.
         *
         * @return the repository singleton.
         */
        fun getRepository(context: Context): BillingRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = BillingRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }

    val newPurchases: Flow<List<String>>

    /**
     * Returns whether or not the user has purchased ProMode.
     * @return a Flow that observes the product purchase state
     */
    val isProModePurchased: Flow<Boolean>

    /**
     * Returns whether or not the user can purchase a product.
     * @return a Flow that observes the ProMode purchase state
     */
    val canPurchaseProMode: Flow<Boolean>

    /** @return the PlayStore name of the pro mode. */
    val proModeTitle: Flow<String>
    /** @return the PlayStore price of the pro mode. */
    val proModePrice: Flow<String>
    /** @return the PlayStore description of the pro mode. */
    val proModeDescription: Flow<String>

    /**
     * Returns a Flow that reports if a billing flow is in process, meaning that launchBillingFlow has returned
     * BillingResponseCode.OK and onPurchasesUpdated hasn't yet been called.
     *
     * @return Flow that indicates the known state of the billing flow.
     */
    val isBillingFlowInProcess: Flow<Boolean>

    /**
     * Launch the billing flow.
     *
     * @param activity active activity to launch our billing flow from.
     * @return true if launch is successful.
     */
    fun launchBillingFlow(activity: Activity)
}