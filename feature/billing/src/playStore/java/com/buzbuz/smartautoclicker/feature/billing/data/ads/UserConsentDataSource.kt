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
package com.buzbuz.smartautoclicker.feature.billing.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.feature.billing.BuildConfig

import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserConsentDataSource @Inject constructor() {

    private val consentInformation: MutableStateFlow<ConsentInformation?> =
        MutableStateFlow(null)

    val isUserConsentingForAds: Flow<Boolean> =
        consentInformation.map { info -> info?.canRequestAds() ?: false }

    val isPrivacyOptionsRequired: Flow<Boolean> =
        consentInformation.map { info ->
            info?.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        }

    fun requestUserConsent(activity: Activity) {
        Log.d(TAG, "Requesting user consent...")

        val params = ConsentRequestParameters.Builder()
        if (BuildConfig.DEBUG) {
            getConsentDebugSettings(activity)?.let(params::setConsentDebugSettings)
        }

        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(
            activity,
            params.build(),
            { onUserConsentInfoUpdated(activity, consentInfo) },
            { Log.w(TAG, "User consent info update failure: [${it.errorCode}] ${it.message}") },
        )
    }

    fun showPrivacyOptionsForm(activity: Activity) {
        Log.d(TAG, "Showing privacy options form")
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let {
                Log.w(TAG, "User consent failure: [${formError.errorCode}] ${formError.message}")
            }
        }
    }

    private fun getConsentDebugSettings(context: Context): ConsentDebugSettings? {
        if (BuildConfig.CONSENT_TEST_DEVICES_IDS.isNullOrEmpty()) return null

        return ConsentDebugSettings.Builder(context)
            .apply {
                BuildConfig.CONSENT_TEST_DEVICES_IDS.forEach { testDeviceId ->
                    Log.d(TAG, "Using $testDeviceId as test device id")
                    addTestDeviceHashedId(testDeviceId)
                }
            }
            .build()
    }

    private fun onUserConsentInfoUpdated(activity: Activity, consentInfo: ConsentInformation) {
        Log.d(TAG, "User content info updated, load and show consent form if required...")

        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
            onUserConsentFormDismissed(consentInfo, error)
        }
    }

    private fun onUserConsentFormDismissed(consentInfo: ConsentInformation, loadAndShowError: FormError?) {
        if (loadAndShowError != null) {
            Log.w(TAG, "User consent failure: [${loadAndShowError.errorCode}] ${loadAndShowError.message}")
        }

        consentInformation.value = consentInfo
        Log.d(TAG, "Updated user consent information, can request ads: ${consentInfo.canRequestAds()}, " +
                "settings required: ${consentInfo.privacyOptionsRequirementStatus}")
    }
}

private const val TAG = "UserConsentDataSource"