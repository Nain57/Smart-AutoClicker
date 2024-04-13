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

package com.buzbuz.gradle.convention.utils

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal fun Project.getLibs(): VersionCatalogWrapper =
    VersionCatalogWrapper(extensions.getByType<VersionCatalogsExtension>().named("libs"))

internal class VersionCatalogWrapper(private val libs: VersionCatalog) {

    val plugins = Plugins()
    val versions = Versions()

    internal inner class Plugins {
        val androidApplication: String
            get() = libs.getPluginId("androidApplication")
        val androidLibrary: String
            get() = libs.getPluginId("androidLibrary")
        val androidxRoom: String
            get() = libs.getPluginId("androidxRoom")
        val jetbrainsKotlinAndroid: String
            get() = libs.getPluginId("jetbrainsKotlinAndroid")
        val jetbrainsKotlinSerialization: String
            get() = libs.getPluginId("jetbrainsKotlinSerialization")
        val googleKsp: String
            get() = libs.getPluginId("googleKsp")
        val googleDaggerHiltAndroid: String
            get() = libs.getPluginId("googleDaggerHiltAndroid")
        val googleCrashlytics: String
            get() = libs.getPluginId("googleCrashlytics")
        val googleGms: String
            get() = libs.getPluginId("googleGms")

        private fun VersionCatalog.getPluginId(alias: String): String =
            findPlugin(alias).get().get().pluginId
    }

    internal inner class Versions {
        val androidCompileSdk: Int
            get() = libs.getVersion("androidCompileSdk")
        val androidMinSdk: Int
            get() = libs.getVersion("androidMinSdk")

        private fun VersionCatalog.getVersion(alias: String): Int =
            findVersion(alias).get().requiredVersion.toInt()
    }

    fun getLibrary(alias: String): Provider<MinimalExternalModuleDependency> =
        libs.findLibrary(alias).get()
}











