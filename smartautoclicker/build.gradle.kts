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
    alias(libs.plugins.buzbuz.androidApplication)
    alias(libs.plugins.buzbuz.buildParameters)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker"
    buildFeatures.viewBinding = true

    defaultConfig {
        applicationId = "com.buzbuz.smartautoclicker"

        versionCode = 41
        versionName = "2.4.2"
    }

    signingConfigs {
        create("release") {
            storeFile = file("./smartautoclicker.jks")
            storePassword = buildParameters["signingStorePassword"].value
            keyAlias = buildParameters["signingKeyAlias"].value
            keyPassword = buildParameters["signingKeyPassword"].value
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    flavorDimensions += listOf("version")
    productFlavors {
        create("fDroid") {
            dimension = "version"
        }
        create("playStore") {
            dimension = "version"
        }
    }
}

// Only apply gms/firebase plugins if we are building for the play store
if (buildParameters.isBuildForVariant("playStoreRelease")) {
    apply { plugin(libs.plugins.buzbuz.crashlytics.get().pluginId) }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.recyclerView)
    implementation(libs.androidx.fragment.ktx)

    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.common.java8)

    implementation(libs.airbnb.lottie)
    implementation(libs.google.material)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:bitmaps"))
    implementation(project(":core:common:display"))
    implementation(project(":core:common:ui"))
    implementation(project(":core:dumb"))
    implementation(project(":core:smart:detection"))
    implementation(project(":core:smart:domain"))
    implementation(project(":core:smart:processing"))
    implementation(project(":feature:backup"))
    implementation(project(":feature:billing"))
    implementation(project(":feature:smart-config"))
    implementation(project(":feature:smart-debugging"))
    implementation(project(":feature:dumb-config"))
    implementation(project(":feature:tutorial"))
}
