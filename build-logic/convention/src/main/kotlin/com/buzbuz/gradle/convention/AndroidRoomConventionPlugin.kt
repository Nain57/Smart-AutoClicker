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

import androidx.room.gradle.RoomExtension

import com.buzbuz.gradle.convention.utils.getLibs
import com.buzbuz.gradle.convention.utils.implementation
import com.buzbuz.gradle.convention.utils.ksp
import com.buzbuz.gradle.convention.utils.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val libs = getLibs()

        plugins {
            apply(libs.plugins.androidxRoom)
            apply(libs.plugins.googleKsp)
        }

        extensions.configure<RoomExtension> {
            schemaDirectory("$projectDir/schemas")
        }

        dependencies {
            implementation(libs.getLibrary("androidx.room.runtime"))
            implementation(libs.getLibrary("androidx.room.ktx"))
            ksp(libs.getLibrary("androidx.room.compiler"))
        }
    }
}