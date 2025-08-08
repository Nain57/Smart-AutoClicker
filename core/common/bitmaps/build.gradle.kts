
plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.bitmaps"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":core:common:base"))
}