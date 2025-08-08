

plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.ui"
    buildFeatures.viewBinding = true
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.recyclerView)

    implementation(libs.google.material)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:display"))
}
