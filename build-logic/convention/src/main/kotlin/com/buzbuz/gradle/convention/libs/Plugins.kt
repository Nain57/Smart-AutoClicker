/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.gradle.convention.libs

import org.gradle.api.artifacts.VersionCatalog


internal class Plugins internal constructor(private val libs: VersionCatalog) {

    val android: Android by lazy { Android() }
    val androidX: AndroidX by lazy { AndroidX() }
    val jetBrains: JetBrains by lazy { JetBrains() }
    val google: Google by lazy { Google() }

    inner class Android internal constructor() {
        val application: String
            get() = libs.getPluginId("androidApplication")
        val library: String
            get() = libs.getPluginId("androidLibrary")
    }

    inner class AndroidX internal constructor() {
        val room: String
            get() = libs.getPluginId("androidxRoom")
    }

    inner class JetBrains internal constructor() {

        val kotlin: Kotlin by lazy { Kotlin() }

        inner class Kotlin {
            val android: String
                get() = libs.getPluginId("jetbrainsKotlinAndroid")
            val serialization: String
                get() = libs.getPluginId("jetbrainsKotlinSerialization")
        }
    }

    inner class Google internal constructor() {
        val ksp: String
            get() = libs.getPluginId("googleKsp")
        val daggerHiltAndroid: String
            get() = libs.getPluginId("googleDaggerHiltAndroid")
        val crashlytics: String
            get() = libs.getPluginId("googleCrashlytics")
        val gms: String
            get() = libs.getPluginId("googleGms")
    }

    private fun VersionCatalog.getPluginId(alias: String): String =
        findPlugin(alias).get().get().pluginId
}