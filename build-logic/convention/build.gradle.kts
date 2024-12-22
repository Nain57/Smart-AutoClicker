/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

        register("androidSigning") {
            id = "com.buzbuz.gradle.android.signing"
            implementationClass = "com.buzbuz.gradle.convention.AndroidSigningConvention"
        }

        register("crashlytics") {
            id = "com.buzbuz.gradle.crashlytics"
            implementationClass = "com.buzbuz.gradle.convention.CrashlyticsConventionPlugin"
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
