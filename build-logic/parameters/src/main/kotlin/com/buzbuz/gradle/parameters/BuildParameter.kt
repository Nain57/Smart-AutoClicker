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
package com.buzbuz.gradle.parameters

import org.gradle.api.Project
import java.io.FileInputStream
import java.util.Properties


class BuildParameter<T : Any>(
    internal val rootProject: Project,
    internal val name: String,
    defaultValue: T,
) {
    val stringValue: String =
        rootProject.getPropertyStringValue(name, defaultValue)

    val typedValue: T =
        stringValue.toTypedValue(defaultValue::class)
}

/**
 * Get the build parameter value from the following sources, in order (retrieves the first found):
 * - Gradle command line arguments (-PargumentName=value).
 * - [LOCAL_PROPERTIES_FILE], with the regular gradle properties syntax.
 * - The [defaultValue] provided as this method parameter.
 */
private fun <T : Any> Project.getPropertyStringValue(parameterName: String, defaultValue: T): String =
    if (rootProject.hasProperty(parameterName)) {
        // Check in gradle command line arguments
        val gradleArg = rootProject.properties[parameterName] as? String
        gradleArg?.sanitizeGradleCommandLineArgument() ?: gradleArg
    } else {
        // Check in local properties file
        val localPropertiesFile = rootProject.file(LOCAL_PROPERTIES_FILE)
        if (localPropertiesFile.exists()) {
            val localProperties = Properties().apply { load(FileInputStream(localPropertiesFile)) }
            localProperties[parameterName] as? String
        } else null
    }  ?: defaultValue.toStringValue()

private const val LOCAL_PROPERTIES_FILE = "local.properties"