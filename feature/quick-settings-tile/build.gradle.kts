plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.feature.qstile"
    buildFeatures.viewBinding = true
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.datastore)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.google.material)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:display"))
    implementation(project(":core:common:settings"))
    implementation(project(":core:common:ui"))
    implementation(project(":core:smart:domain"))
    implementation(project(":core:smart:processing"))
    implementation(project(":core:common:permissions"))
}
