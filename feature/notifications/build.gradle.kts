plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.feature.notifications"
    buildFeatures.viewBinding = true
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        vectorDrawables { useSupportLibrary = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation(libs.androidx.appCompat)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:permissions"))
    implementation(project(":core:common:settings"))
    implementation(project(":core:common:ui"))
    implementation(project(":core:smart:domain"))
}