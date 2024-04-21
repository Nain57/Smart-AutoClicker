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
    alias(libs.plugins.buzbuz.kotlinSerialization)
    alias(libs.plugins.buzbuz.androidUnitTest)
    alias(libs.plugins.buzbuz.hilt)
}

android {
    namespace = "com.buzbuz.smartautoclicker.feature.backup"
    buildFeatures.viewBinding = true
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    implementation(libs.google.material)

    implementation(project(":core:common:base"))
    implementation(project(":core:common:bitmaps"))
    implementation(project(":core:common:display"))
    implementation(project(":core:common:ui"))
    implementation(project(":core:smart:database"))
    implementation(project(":core:smart:domain"))
    implementation(project(":core:dumb"))

    testImplementation(libs.kotlinx.coroutines.test)
}