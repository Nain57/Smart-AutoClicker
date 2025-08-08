

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.capture"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core.ktx)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:bitmaps"))
}