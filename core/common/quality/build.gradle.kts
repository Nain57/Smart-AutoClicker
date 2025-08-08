

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.common.quality"

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore)

    implementation(libs.androidx.fragment.ktx)
    implementation(libs.google.material)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:ui"))

    testImplementation(libs.kotlinx.coroutines.test)
}
