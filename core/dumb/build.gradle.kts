
plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
    alias(libs.plugins.buzbuz.androidRoom)
    alias(libs.plugins.buzbuz.flavour)
    alias(libs.plugins.buzbuz.kotlinSerialization)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.smartautoclicker.core.dumb"

    sourceSets {
        getByName("test") {
            // Adds exported schema location as test app assets.
            assets.srcDirs("$projectDir/schemas")
        }
    }
}

dependencies {
    implementation(project(":core:common:base"))
    implementation(project(":core:common:settings"))
}