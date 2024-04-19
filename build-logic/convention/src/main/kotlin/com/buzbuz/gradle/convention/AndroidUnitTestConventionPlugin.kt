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
package com.buzbuz.gradle.convention

import com.buzbuz.gradle.convention.utils.android
import com.buzbuz.gradle.convention.utils.getLibs

import com.buzbuz.gradle.convention.utils.plugins
import com.buzbuz.gradle.convention.utils.testImplementation

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidUnitTestConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val libs = getLibs()

        plugins {
            apply(libs.plugins.jetbrainsKotlinAndroid)
        }

        android {
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            testOptions.unitTests.isIncludeAndroidResources = true
        }

        dependencies {
            testImplementation(libs.getLibrary("junit"))
            testImplementation(libs.getLibrary("androidx.test.core"))
            testImplementation(libs.getLibrary("androidx.test.ext.junit"))
            testImplementation(libs.getLibrary("mockito.core"))
            testImplementation(libs.getLibrary("mockito.kotlin"))
            testImplementation(libs.getLibrary("mockk.android"))
            testImplementation(libs.getLibrary("robolectric"))
        }
    }
}