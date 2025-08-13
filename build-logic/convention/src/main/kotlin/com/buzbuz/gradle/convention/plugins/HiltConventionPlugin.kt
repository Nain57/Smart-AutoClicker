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
package com.buzbuz.gradle.convention.plugins

import com.buzbuz.gradle.convention.extensions.getLibs
import com.buzbuz.gradle.convention.extensions.implementation
import com.buzbuz.gradle.convention.extensions.ksp
import com.buzbuz.gradle.convention.extensions.kspTest
import com.buzbuz.gradle.convention.extensions.testImplementation
import com.buzbuz.gradle.convention.extensions.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class HiltConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val libs = getLibs()

        plugins {
            apply(libs.plugins.google.ksp)
            apply(libs.plugins.google.daggerHiltAndroid)
        }

        dependencies {
            implementation(libs.getLibrary("google.dagger.hilt"))
            ksp(libs.getLibrary("google.dagger.hilt.compiler"))

            testImplementation(libs.getLibrary("google.dagger.hilt.testing"))
            kspTest(libs.getLibrary("google.dagger.hilt.compiler"))
        }
    }
}