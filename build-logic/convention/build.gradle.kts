

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.buzbuz.gradle.buildlogic.convention"

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
        register("androidApplication") {
            id = "com.buzbuz.gradle.android.application"
            implementationClass = "com.buzbuz.gradle.convention.AndroidApplicationConventionPlugin"
        }

        register("androidLibrary") {
            id = "com.buzbuz.gradle.android.library"
            implementationClass = "com.buzbuz.gradle.convention.AndroidLibraryConventionPlugin"
        }

        register("androidRoom") {
            id = "com.buzbuz.gradle.android.room"
            implementationClass = "com.buzbuz.gradle.convention.AndroidRoomConventionPlugin"
        }

        register("androidUnitTest") {
            id = "com.buzbuz.gradle.android.unittest"
            implementationClass = "com.buzbuz.gradle.convention.AndroidUnitTestConventionPlugin"
        }

        register("androidLocalTest") {
            id = "com.buzbuz.gradle.android.localtest"
            implementationClass = "com.buzbuz.gradle.convention.AndroidLocalTestConventionPlugin"
        }

        register("androidSigning") {
            id = "com.buzbuz.gradle.android.signing"
            implementationClass = "com.buzbuz.gradle.convention.AndroidSigningConvention"
        }

        register("crashlytics") {
            id = "com.buzbuz.gradle.crashlytics"
            implementationClass = "com.buzbuz.gradle.convention.CrashlyticsConventionPlugin"
        }

        register("flavour") {
            id = "com.buzbuz.gradle.android.flavour"
            implementationClass = "com.buzbuz.gradle.convention.FlavourConventionPlugin"
        }

        register("androidHilt") {
            id = "com.buzbuz.gradle.android.hilt"
            implementationClass = "com.buzbuz.gradle.convention.HiltConventionPlugin"
        }

        register("kotlinSerialization") {
            id = "com.buzbuz.gradle.android.kotlin.serialization"
            implementationClass = "com.buzbuz.gradle.convention.KotlinSerializationConventionPlugin"
        }
    }
}
