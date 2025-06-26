/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.gradle.parameters

import com.buzbuz.gradle.core.model.KlickrBuildType
import com.buzbuz.gradle.core.extensions.isBuildForVariant
import org.gradle.api.Project
import org.gradle.api.provider.Property

abstract class BuildParametersPluginExtension {

    abstract val rootProject: Property<Project>

    /** Tells if the application ID and components should be randomized at build time. */
    val randomizeAppId: BuildParameter<Boolean> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "randomizeAppId",
            defaultValue = false,
        )
    }

    /** Release signing configuration store password. */
    val signingStorePassword: BuildParameter<String> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "signingStorePassword",
            defaultValue = "",
        )
    }

    /** Release signing configuration key alias. */
    val signingKeyAlias: BuildParameter<String> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "signingKeyAlias",
            defaultValue = "",
        )
    }

    /** Release signing configuration key password. */
    val signingKeyPassword: BuildParameter<String> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "signingKeyPassword",
            defaultValue = "",
        )
    }

    /**
     * The devices to use as test device for the User Messaging SDK in debug builds only.
     * To get the ID of a device, launch the app and request the user consent. The device ID will be printed in
     * the logcat; see https://developers.google.com/admob/android/privacy?hl=fr#testing
     */
    val consentTestDevicesIds: BuildParameter<Array<String>> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "consentTestDevicesIds",
            defaultValue = emptyArray(),
        )
    }

    /**
     * The geographical area to use for the User Messaging SDK in debug builds only.
     * Must be one of the values defined in ConsentDebugSettings.DebugGeography:
     *  - DEBUG_GEOGRAPHY_DISABLED: 0
     *  - DEBUG_GEOGRAPHY_EEA:      1
     *  - DEBUG_GEOGRAPHY_NOT_EEA:  2
     */
    val consentTestGeography: BuildParameter<Int> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "consentTestGeography",
            defaultValue = 0,
        )
    }

    /** The identifier for the application for the AdMob SDK */
    val adsApplicationId: BuildParameter<String> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "adsApplicationId",
            defaultValue = "",
        )
    }

    /**
     * The advertisement block id for the AdMob SDK.
     * Should have the following syntax: ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
     *
     * For debug builds, you can use one of the following Sample ad unit ID:
     *  - Interstitial:         ca-app-pub-3940256099942544/1033173712
     *  - Interstitial Video:   ca-app-pub-3940256099942544/8691691433
     *
     * Note: If you are using the emulator or a physical device defined in adsTestDevicesIds, you can use your
     * production ad unit ID.
     */
    val adsUnitId: BuildParameter<String> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "adsUnitId",
            defaultValue =
                if (rootProject.get().isBuildForVariant(buildType = KlickrBuildType.RELEASE)) ""
                else "ca-app-pub-3940256099942544/8691691433",
        )
    }

    /**
     * The devices to use as test device for the AdMob SDK in debug builds only.
     * If you are using your production adsApplicationId, you need to define all your physical test devices here
     * To get the ID of a device, launch the app and goes to the paywall in order to load an ad. The device ID
     * will be printed in the logcat; see https://developers.google.com/admob/android/test-ads?hl=fr#add_your_test_device_programmatically
     */
    val adsTestDevicesIds: BuildParameter<Array<String>> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "adsTestDevicesIds",
            defaultValue = emptyArray(),
        )
    }

    /**
     * The application billing public key.
     *
     * You currently get this from the Google Play developer console under the "Monetization Setup" category
     * in the Licensing area.
     */
    val billingPublicKey: BuildParameter<String> by lazy {
        BuildParameter(
            rootProject = rootProject.get(),
            name = "billingPublicKey",
            defaultValue = "",
        )
    }
}
