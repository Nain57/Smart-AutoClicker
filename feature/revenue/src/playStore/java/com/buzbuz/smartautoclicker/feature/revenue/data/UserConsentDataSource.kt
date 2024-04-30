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

import android.app.Activity
import android.content.Context
import android.util.Log

import com.buzbuz.smartautoclicker.feature.revenue.BuildConfig

import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserConsentDataSource @Inject constructor() {

    private val _isUserConsentingForAds: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isUserConsentingForAds: StateFlow<Boolean> = _isUserConsentingForAds

    private val _isPrivacyOptionsRequired: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isPrivacyOptionsRequired: Flow<Boolean> = _isPrivacyOptionsRequired

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

        refreshConsentInfo(consentInfo)
    }

    fun showPrivacyOptionsForm(activity: Activity) {
        Log.d(TAG, "Showing privacy options form")
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            formError?.let {
                Log.w(TAG, "User consent failure: [${formError.errorCode}] ${formError.message}")
            }

            refreshConsentInfo(UserMessagingPlatform.getConsentInformation(activity))
        }
    }

    private fun onUserConsentInfoUpdated(activity: Activity, consentInfo: ConsentInformation) {
        Log.d(TAG, "User content info updated, load and show consent form if required...")

        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
            if (error != null) {
                Log.w(TAG, "User consent failure: [${error.errorCode}] ${error.message}")
            }

            refreshConsentInfo(consentInfo)
        }
    }

    private fun refreshConsentInfo(consentInfo: ConsentInformation) {
        _isUserConsentingForAds.value = consentInfo.canRequestAds()
        _isPrivacyOptionsRequired.value =
            consentInfo.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

        Log.d(
            TAG, "Updated user consent information, can request ads: ${consentInfo.canRequestAds()}, " +
                "settings required: ${consentInfo.privacyOptionsRequirementStatus}")
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
}

private const val TAG = "UserConsentDataSource"