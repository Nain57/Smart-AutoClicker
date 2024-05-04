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
package com.buzbuz.smartautoclicker.feature.revenue.data

import android.content.Context
import android.util.Log
import com.buzbuz.smartautoclicker.feature.revenue.BuildConfig
import com.google.android.ump.ConsentDebugSettings


internal fun getConsentDebugSettings(context: Context): ConsentDebugSettings? {
    if (!BuildConfig.DEBUG || BuildConfig.CONSENT_TEST_DEVICES_IDS.isNullOrEmpty()) return null

    return ConsentDebugSettings.Builder(context)
        .apply {
            BuildConfig.CONSENT_TEST_DEVICES_IDS.forEach { testDeviceId ->
                Log.d(TAG, "Using $testDeviceId as consent test device id")
                addTestDeviceHashedId(testDeviceId)
            }

            @Suppress("KotlinConstantConditions")
            if (BuildConfig.CONSENT_TEST_GEOGRAPHY != ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED) {
                Log.d(TAG, "Using consent test geography ${BuildConfig.CONSENT_TEST_GEOGRAPHY}")
                setDebugGeography(DEBUG_CONSENT_GEOGRAPHY)
            }
        }
        .build()
}

internal fun getAdsDebugTestDevicesIds() : List<String>? {
    if (!BuildConfig.DEBUG || BuildConfig.ADS_TEST_DEVICES_IDS.isNullOrEmpty()) return null

    return buildList {
        BuildConfig.ADS_TEST_DEVICES_IDS.forEach { testDeviceId ->
            Log.d(TAG, "Using $testDeviceId as ads test device id")
            add(testDeviceId)
        }
    }
}


@ConsentDebugSettings.DebugGeography
private const val DEBUG_CONSENT_GEOGRAPHY: Int =
    ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED

private const val TAG = "RevenueTestConfiguration"