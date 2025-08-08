

import com.buzbuz.gradle.obfuscation.getExtraActualApplicationId

plugins {
    alias(libs.plugins.buzbuz.androidApplication)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.obfuscation)
    alias(libs.plugins.buzbuz.buildParameters)
    alias(libs.plugins.buzbuz.hilt)
}

obfuscationConfig {
    obfuscatedApplication {
        create("com.buzbuz.smartautoclicker.application.SmartAutoClickerApplication")
    }
    obfuscatedComponents {
        create("com.buzbuz.smartautoclicker.scenarios.ScenarioActivity")
        create("com.buzbuz.smartautoclicker.SmartAutoClickerService")
    }

    setup(
        applicationId = "com.buzbuz.smartautoclicker",
        appNameResId = "@string/app_name",
        shouldRandomize = buildParameters["randomizeAppId"].asBoolean() &&
                buildParameters.isBuildForVariant("fDroid"),
    )
}

android {
    namespace = "com.buzbuz.smartautoclicker"

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = getExtraActualApplicationId()

        versionCode = 78
        versionName = "3.3.10"
    }

    if (buildParameters.isBuildForVariant("fDroidDebug")) {
        buildTypes {
            debug {
                applicationIdSuffix = ".debug"
            }
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("./smartautoclicker.jks")
            storePassword = buildParameters["signingStorePassword"].asString()
            keyAlias = buildParameters["signingKeyAlias"].asString()
            keyPassword = buildParameters["signingKeyPassword"].asString()
        }
    }
}

// Apply signature convention after declaring the signingConfigs
apply { plugin(libs.plugins.buzbuz.androidSigning.get().pluginId) }

// Only apply gms/firebase plugins if we are building for the play store
if (buildParameters.isBuildForVariant("playStoreRelease")) {
    apply { plugin(libs.plugins.buzbuz.crashlytics.get().pluginId) }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore)
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
    implementation(project(":core:common:overlays"))
    implementation(project(":core:common:permissions"))
    implementation(project(":core:common:quality"))
    implementation(project(":core:common:settings"))
    implementation(project(":core:common:ui"))
    implementation(project(":core:smart:detection"))
    implementation(project(":core:smart:domain"))
    implementation(project(":core:smart:processing"))
    implementation(project(":feature:backup"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:quick-settings-tile"))
    implementation(project(":feature:smart-config"))
    implementation(project(":feature:smart-debugging"))
}
