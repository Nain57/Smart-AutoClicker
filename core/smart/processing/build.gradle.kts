

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.processing"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:bitmaps"))
    implementation(project(":core:common:display"))
    implementation(project(":core:common:settings"))
    implementation(project(":core:smart:detection"))
    implementation(project(":core:smart:domain"))

    testImplementation(libs.kotlinx.coroutines.test)
}