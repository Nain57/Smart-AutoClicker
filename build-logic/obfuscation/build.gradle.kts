

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.buzbuz.gradle.buildlogic.obfuscation"

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.androidx.room.gradlePlugin)
    compileOnly(libs.google.firebase.crashlytics.gradlePlugin)
    compileOnly(libs.google.gms.gradlePlugin)
    implementation(project(":core"))
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("obfuscation") {
            id = "com.buzbuz.gradle.obfuscation"
            implementationClass = "com.buzbuz.gradle.obfuscation.ObfuscationPlugin"
        }
    }
}
