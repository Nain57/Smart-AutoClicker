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

import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalog

class Versions internal constructor(private val libs: VersionCatalog) {

    val android: Android by lazy { Android() }

    inner class Android internal constructor() {
        val compileSdk: Int
            get() = libs.getVersion("androidCompileSdk")
        val minSdk: Int
            get() = libs.getVersion("androidMinSdk")
    }

    val java: JavaVersion
        get() = JavaVersion.valueOf("VERSION_${libs.getVersion("java")}")

    val jvmTarget: String
        get() = libs.getStringVersion("java")


    private fun VersionCatalog.getVersion(alias: String): Int =
        getStringVersion(alias).toInt()

    private fun VersionCatalog.getStringVersion(alias: String): String =
        findVersion(alias).get().requiredVersion
}