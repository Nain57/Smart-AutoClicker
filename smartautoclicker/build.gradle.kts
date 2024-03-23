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
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

plugins {
    alias(libs.plugins.buzbuz.androidApplication)
    alias(libs.plugins.buzbuz.buildParameters)
    alias(libs.plugins.googleKsp)
}

// Only apply gms/firebase plugins if we are building for the play store
val isPlayStoreBuild = buildParameters.isBuildForFlavour("playStore")
if (isPlayStoreBuild) {
    apply {
        plugin(libs.plugins.googleGms.get().pluginId)
        plugin(libs.plugins.googleCrashlytics.get().pluginId)
    }
}

val signingStoreFile = file("./smartautoclicker.jks")
val signingProperties = Properties()

android {
    namespace = "com.buzbuz.smartautoclicker"
    buildFeatures.viewBinding = true

    defaultConfig {
        applicationId = "com.buzbuz.smartautoclicker"

        versionCode = 41
        versionName = "2.4.2"
    }

    compileOptions {
        kotlin {
            kotlinOptions {
                freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
            }
        }
    }

    if (signingStoreFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = signingStoreFile
                storePassword = buildParameters["signingStorePassword"]
                keyAlias = buildParameters["signingKeyAlias"]
                keyPassword = buildParameters["signingKeyPassword"]
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (signingStoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            if (isPlayStoreBuild) {
                configure<CrashlyticsExtension> {
                    nativeSymbolUploadEnabled = true
                    unstrippedNativeLibsDir = "build/intermediates/merged_native_libs/playStoreRelease/out/lib"
                }
            }
        }

        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }

    // Specifies one flavor dimension.
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

    // Google Play Store version dependencies only
    "playStoreImplementation"(platform(libs.google.firebase.bom))
    "playStoreImplementation"(libs.google.firebase.crashlytics.ktx)
    "playStoreImplementation"(libs.google.firebase.crashlytics.ndk)

    implementation(project(":core:base"))
    implementation(project(":core:detection"))
    implementation(project(":core:display"))
    implementation(project(":core:domain"))
    implementation(project(":core:dumb"))
    implementation(project(":core:processing"))
    implementation(project(":core:ui"))
    implementation(project(":feature:backup"))
    implementation(project(":feature:billing"))
    implementation(project(":feature:floating-menu"))
    implementation(project(":feature:scenario-config"))
    implementation(project(":feature:scenario-config-dumb"))
    implementation(project(":feature:scenario-debugging"))
    implementation(project(":feature:tutorial"))
}

project.afterEvaluate {
    tasks.filter { task ->
        task.name.startsWith("generateCrashlyticsSymbolFilePlayStoreRelease")
    }.forEach {task ->
        task.dependsOn("mergePlayStoreReleaseNativeLibs")
    }
}
