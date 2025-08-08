
plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.feature.notifications"
    buildFeatures.viewBinding = true
}

dependencies {
    implementation(libs.androidx.appCompat)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:permissions"))
    implementation(project(":core:common:settings"))
    implementation(project(":core:common:ui"))
    implementation(project(":core:smart:domain"))
}