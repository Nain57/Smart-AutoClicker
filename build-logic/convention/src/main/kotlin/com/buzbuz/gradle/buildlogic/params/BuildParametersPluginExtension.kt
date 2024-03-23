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
package com.buzbuz.gradle.buildlogic.params

import com.android.build.api.dsl.LibraryProductFlavor
import org.gradle.api.Project
import org.gradle.api.provider.Property
import java.io.FileInputStream
import java.util.Properties

abstract class BuildParametersPluginExtension {

    abstract val project: Property<Project>

    @Suppress("UNNECESSARY_SAFE_CALL")
    operator fun get(propertyName: String): String? =
        project.get()?.rootProject?.let { project ->
            val localPropertiesValue: String? =
                project.file("local.properties").let { localPropertiesFile ->
                    if (localPropertiesFile.exists()) {
                        val localProperties = Properties().apply { load(FileInputStream(localPropertiesFile)) }
                        localProperties[propertyName] as? String
                    } else null
                }
            if (!localPropertiesValue.isNullOrBlank()) return localPropertiesValue

            if (project.hasProperty(propertyName)) {
                project.properties[propertyName] as? String
            } else {
                project.logger.warn("WARNING: Build property $propertyName was not found !")
                null
            }
        }

    fun setAsStringBuildConfigField(flavor: LibraryProductFlavor, configName: String, paramName: String) {
        flavor.buildConfigField(
            type = "String",
            name = configName,
            value = "\"${get(paramName)}\""
        )
    }
}
