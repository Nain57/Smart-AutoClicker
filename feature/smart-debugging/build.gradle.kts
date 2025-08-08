

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.feature.smart.debugging"
    buildFeatures.viewBinding = true
}

dependencies {

    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.recyclerView)

    implementation(libs.google.material)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:bitmaps"))
    implementation(project(":core:common:overlays"))
    implementation(project(":core:common:ui"))
    implementation(project(":core:smart:detection"))
    implementation(project(":core:smart:domain"))
    implementation(project(":core:smart:processing"))
}