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

import org.gradle.api.Project
import java.io.FileInputStream
import java.util.Properties

/**
 * Get a build property value.
 * Tries to retrieve it first from the the local.properties file. If undefined, retrieves it from the command line arguments.
 *
 * Note: Command line arguments are provided as following: ./gradlew -PpropertyName="value"
 */
fun Project.buildProperty(propertyName: String): String? {
    val localPropertiesValue: String? =
        rootProject.file("local.properties").let { localPropertiesFile ->
            if (localPropertiesFile.exists()) {
                val localProperties = Properties().apply { load(FileInputStream(localPropertiesFile)) }
                localProperties[propertyName] as? String
            } else null
        }
    if (!localPropertiesValue.isNullOrBlank()) return localPropertiesValue

    return if (rootProject.hasProperty(propertyName)) {
        rootProject.properties[propertyName] as? String
    } else {
        logger.warn("WARNING: Build property $propertyName was not found !")
        null
    }
}
