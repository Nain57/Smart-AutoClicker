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

abstract class IAdsRepository: Dumpable {

    abstract val isUserConsentingForAds: Flow<Boolean>
    abstract val isPrivacyOptionsRequired: Flow<Boolean>

    abstract fun requestUserConsentIfNeeded(activity: Activity)
    abstract fun showPrivacyOptionsForm(activity: Activity)

    abstract fun loadAd(context: Context)
    abstract fun showAd(activity: Activity)

    override fun dump(writer: PrintWriter, prefix: CharSequence) {
        val contentPrefix = prefix.addDumpTabulationLvl()

        writer.apply {
            append(prefix).println("* AdsRepository:")
            append(contentPrefix)
                .append("- adsConsent=${isUserConsentingForAds.dumpWithTimeout()}; ")
                .append("privacyOptionsRequired=${isPrivacyOptionsRequired.dumpWithTimeout()}; ")
                .println()
        }
    }
}