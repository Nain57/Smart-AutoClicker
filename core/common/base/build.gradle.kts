import com.buzbuz.gradle.core.playStoreImplementation



plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.kotlinSerialization)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.extensions"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.reflect)

    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.room.ktx)

    implementation(libs.google.material)

    testImplementation(libs.kotlinx.coroutines.test)

    playStoreImplementation(libs.google.firebase.crashlytics.ktx)
}
