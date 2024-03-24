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

import com.buzbuz.gradle.convention.utils.androidLib
import com.buzbuz.gradle.convention.utils.getPluginId
import com.buzbuz.gradle.convention.utils.getVersion
import com.buzbuz.gradle.convention.utils.libs
import com.buzbuz.gradle.convention.utils.plugins

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        plugins {
            apply(libs.getPluginId("androidLibrary"))
            apply(libs.getPluginId("jetbrainsKotlinAndroid"))
        }

        androidLib {
            compileSdk = libs.getVersion("androidCompileSdk")

            defaultConfig.apply {
                targetSdk = libs.getVersion("androidCompileSdk")
                minSdk = libs.getVersion("androidMinSdk")
            }

            compileOptions.apply {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
