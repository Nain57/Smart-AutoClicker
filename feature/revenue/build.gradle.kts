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

import com.buzbuz.gradle.core.extensions.playStore
import com.buzbuz.gradle.parameters.buildConfigField
import com.buzbuz.gradle.parameters.manifestPlaceholders

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.buildParameters)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.feature.revenue"

    productFlavors {
        playStore {
            dimension = "version"

            buildFeatures {
                buildConfig = true
                viewBinding = true
            }

            buildConfigField(buildParameters.consentTestDevicesIds)
            buildConfigField(buildParameters.consentTestGeography)
            buildConfigField(buildParameters.adsUnitId)
            buildConfigField(buildParameters.adsTestDevicesIds)
            buildConfigField(buildParameters.billingPublicKey)

            manifestPlaceholders(buildParameters.adsApplicationId)
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:quality"))
    implementation(project(":core:common:ui"))

    playStoreImplementation(libs.androidx.appCompat)
    playStoreImplementation(libs.androidx.core.ktx)
    playStoreImplementation(libs.androidx.fragment.ktx)
    playStoreImplementation(libs.androidx.lifecycle.viewmodel.ktx)

    playStoreImplementation(libs.android.billingClient)
    playStoreImplementation(libs.android.billingClient.ktx)

    playStoreImplementation(libs.google.userMessaging)
    playStoreImplementation(libs.google.gms.ads)

    playStoreImplementation(libs.google.material)

    testPlayStoreImplementation(libs.kotlinx.coroutines.test)
}
