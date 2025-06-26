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
package com.buzbuz.gradle.convention.plugins

import com.buzbuz.gradle.convention.model.BuildParameters
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import java.io.FileInputStream
import java.util.Properties
import kotlin.reflect.KClass


class BuildParametersPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        extensions.create<BuildParametersPluginExtension>(PLUGIN_EXTENSION_NAME).apply {
            rootProject.set(target.rootProject)
        }
    }

    private companion object {
        const val PLUGIN_EXTENSION_NAME = "buildParameters"
    }
}

abstract class BuildParametersPluginExtension : BuildParameters()


/**
 * Get the build parameter value from the following sources, in order (retrieves the first found):
 * - Gradle command line arguments (-PargumentName=value).
 * - [LOCAL_PROPERTIES_FILE], with the regular gradle properties syntax.
 * - The [defaultValue] provided as this method parameter.
 */
internal fun <T : Any> Project.getPropertyStringValue(parameterName: String, defaultValue: T): String =
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

/** Convert the default values provided as default build arguments their valid gradle representation. */
internal fun <T: Any> T.toStringValue(): String =
    when (this) {
        is Boolean -> this.toString()
        is Int -> this.toString()
        is String -> "$this"
        is Array<*> -> this.fold("{") { acc, value -> "$acc,$value" }.plus("}")
        else -> throw IllegalArgumentException("Unsupported type $this")
    }

/** Convert a gradle string representation of a value to its actual Kotlin type. */
@Suppress("UNCHECKED_CAST")
internal fun <T: Any> String.toTypedValue(kClass: KClass<T>): T =
    when (kClass) {
        Boolean::class -> toBoolean()
        Int::class -> toInt()
        String::class -> this
        Array<String>::class -> removePrefix("{").removeSuffix("}").split(',').toTypedArray()
        else -> throw IllegalArgumentException("Invalid value type $this")
    } as T

/** Sanitize the command line argument if needed. */
private fun String.sanitizeGradleCommandLineArgument(): String =
    when {
        startsWith('{') && endsWith('}') -> sanitizeStringArrayParameter()
        else -> this
    }

private fun String.sanitizeStringArrayParameter(): String =
    removePrefix("{")
        .removeSuffix("}")
        .split(',')
        .joinToString(
            prefix = "{",
            postfix = "}",
            separator = ", ",
            transform = { arrayValue ->
                if (arrayValue.startsWith("\"")) arrayValue
                else "\"$arrayValue\""
            },
        )


private const val LOCAL_PROPERTIES_FILE = "local.properties"