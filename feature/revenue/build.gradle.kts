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
plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.buildParameters)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.feature.revenue"

    // Specifies one flavor dimension.
    flavorDimensions += "version"
    productFlavors {

        create("fDroid") {
            dimension = "version"
        }

        create("playStore") {
            dimension = "version"

            buildFeatures {
                buildConfig = true
                viewBinding = true
            }

            /**
             * The devices to use as test device for the User Messaging SDK in debug builds only.
             * To get the ID of a device, launch the app and request the user consent. The device ID will be printed in
             * the logcat; see https://developers.google.com/admob/android/privacy?hl=fr#testing
             */
            buildParameters["consentTestDevicesIds"].asStringArrayBuildConfigField(this)

            /**
             * The geographical area to use for the User Messaging SDK in debug builds only.
             * Must be one of the values defined in ConsentDebugSettings.DebugGeography:
             *  - DEBUG_GEOGRAPHY_DISABLED: 0
             *  - DEBUG_GEOGRAPHY_EEA:      1
             *  - DEBUG_GEOGRAPHY_NOT_EEA:  2
             */
            buildParameters["consentTestGeography"].asIntBuildConfigField(this, default = 0)

            /**
             * The advertisement id for the AdMob SDK.
             * Should have the following syntax: ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX
             *
             * For debug builds, you can use one of the following Sample ad unit ID:
             *  - Interstitial:         ca-app-pub-3940256099942544/1033173712
             *  - Interstitial Video:   ca-app-pub-3940256099942544/8691691433
             *
             * Note: If you are using the emulator or a physical device defined in adsTestDevicesIds, you can use your
             * production ad unit ID.
             */
            buildParameters["adsUnitId"].apply {
                val defaultAdsUnitId =
                    if (buildParameters.isBuildForVariant("release")) null
                    else "ca-app-pub-3940256099942544/8691691433"
                asManifestPlaceHolder(this@create, default = defaultAdsUnitId)
                asStringBuildConfigField(this@create, default = defaultAdsUnitId)
            }

            /**
             * The devices to use as test device for the AdMob SDK in debug builds only.
             * If you are using your production adsApplicationId, you need to define all your physical test devices here
             * To get the ID of a device, launch the app and goes to the paywall in order to load an ad. The device ID
             * will be printed in the logcat; see https://developers.google.com/admob/android/test-ads?hl=fr#add_your_test_device_programmatically
             */
            buildParameters["adsTestDevicesIds"].asStringArrayBuildConfigField(this)

            /**
             * The application billing public key.
             *
             * You currently get this from the Google Play developer console under the "Monetization Setup" category
             * in the Licensing area.
             */
            buildParameters["billingPublicKey"].asStringBuildConfigField(this)
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:quality"))
    implementation(project(":core:common:ui"))

    "playStoreImplementation"(libs.androidx.appCompat)
    "playStoreImplementation"(libs.androidx.core.ktx)
    "playStoreImplementation"(libs.androidx.fragment.ktx)
    "playStoreImplementation"(libs.androidx.lifecycle.viewmodel.ktx)

    "playStoreImplementation"(libs.android.billingClient)
    "playStoreImplementation"(libs.android.billingClient.ktx)

    "playStoreImplementation"(libs.google.userMessaging)
    "playStoreImplementation"(libs.google.gms.ads)

    "playStoreImplementation"(libs.google.material)

    "testPlayStoreImplementation"(libs.kotlinx.coroutines.test)
}
