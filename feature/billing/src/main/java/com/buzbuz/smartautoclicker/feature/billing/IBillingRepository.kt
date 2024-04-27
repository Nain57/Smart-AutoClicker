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
package com.buzbuz.smartautoclicker.feature.billing

import android.app.Activity
import android.content.Context
import com.buzbuz.smartautoclicker.core.base.Dumpable
import com.buzbuz.smartautoclicker.core.base.addDumpTabulationLvl
import com.buzbuz.smartautoclicker.core.base.dumpWithTimeout

import kotlinx.coroutines.flow.Flow
import java.io.PrintWriter

abstract class IBillingRepository : Dumpable {

    abstract val newPurchases: Flow<List<String>>

    /**
     * Returns whether or not the user has purchased ProMode.
     * @return a Flow that observes the product purchase state
     */
    abstract val isProModePurchased: Flow<Boolean>

    /**
     * Returns whether or not the user can purchase a product.
     * @return a Flow that observes the ProMode purchase state
     */
    abstract val canPurchaseProMode: Flow<Boolean>

    /** @return the PlayStore name of the pro mode. */
    abstract val proModeTitle: Flow<String>
    /** @return the PlayStore price of the pro mode. */
    abstract val proModePrice: Flow<String>
    /** @return the PlayStore description of the pro mode. */
    abstract val proModeDescription: Flow<String>

    /**
     * Returns a Flow that reports if a billing flow is in process.
     *
     * @return Flow that indicates the known state of the billing flow.
     */
    abstract val isBillingFlowInProcess: Flow<Boolean>

    /**
     * Launch the billing activity.
     *
     * @param context the Android context.
     */
    abstract fun startBillingActivity(context: Context)

    internal abstract fun launchPlayStoreBillingFlow(activity: Activity)

    internal abstract fun setBillingActivityState(created: Boolean)

    internal abstract fun isPurchased(): Boolean

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* BillingRepository:")
            append(contentPrefix)
                .append("- canPurchase=${canPurchaseProMode.dumpWithTimeout()}; ")
                .append("isPurchased=${isPurchased()}; ")
                .println()
            append(contentPrefix)
                .append("- title=${proModeTitle.dumpWithTimeout()}; ")
                .append("price=${proModePrice.dumpWithTimeout()}; ")
                .append("description=${proModeDescription.dumpWithTimeout()}; ")
                .println()
        }
    }
}