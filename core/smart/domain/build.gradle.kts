

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.domain"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.room.ktx)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:bitmaps"))
    implementation(project(":core:smart:database"))

    testImplementation(libs.kotlinx.coroutines.test)
}