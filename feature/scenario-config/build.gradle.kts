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
plugins {
    alias(libs.plugins.buzbuz.androidLibrary)
}

android {
    namespace = "com.buzbuz.smartautoclicker.feature.scenario.config"
    buildFeatures.viewBinding = true

    // Specifies one flavor dimension.
    flavorDimensions += "version"
    productFlavors {
        create("fDroid") {
            dimension = "version"
        }
        create("playStore") {
            dimension = "version"
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.recyclerView)

    implementation(libs.google.material)

    implementation(project(":core:android"))
    implementation(project(":core:display"))
    implementation(project(":core:domain"))
    implementation(project(":core:base"))
    implementation(project(":core:processing"))
    implementation(project(":core:ui"))
    implementation(project(":feature:billing"))
    implementation(project(":feature:scenario-debugging"))
}