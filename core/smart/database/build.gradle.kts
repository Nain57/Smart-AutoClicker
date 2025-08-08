
plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.kotlinSerialization)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.androidRoom)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.core.database"

    sourceSets {
        getByName("test") {
            // Adds exported schema location as test app assets.
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

dependencies {
    implementation(project(":core:common:base"))

    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}