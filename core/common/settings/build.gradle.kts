

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.common.settings"
    buildFeatures.viewBinding = true
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore)

    implementation(project(":core:common:base"))
}