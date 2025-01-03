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
package com.buzbuz.gradle.randomizer.component

import com.buzbuz.gradle.randomizer.extensions.ObfuscatedComponentNamedObject
import com.buzbuz.gradle.randomizer.getExtraActualApplicationId
import com.buzbuz.gradle.randomizer.getExtraOriginalApplicationId
import com.buzbuz.gradle.randomizer.nextString
import com.buzbuz.gradle.randomizer.sourceFolderPath
import org.gradle.api.Project
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import kotlin.random.Random


internal data class ObfuscatedComponent(
    val originalComponentName: String,
    val originalPackageName: String,
    val originalFlattenComponentName: String,
    val originalClassName: String,
    val originalFilePath: String,
    val randomizedClassName: String,
    val randomizedFlattenComponentName: String,
    val randomizedFileDirectoryPath: String,
    val randomizedFilePath: String,
    val manifestPlaceholderKey: String,
    val manifestPlaceholderValue: String,
)

internal fun ObfuscatedComponentNamedObject.toDomain(target: Project, random: Random): ObfuscatedComponent {
    val originalPackagePath = name.substringBeforeLast('.').replace('.', '/')
    val originalPackageName = name.substringBeforeLast('.')
    val originalClassName = name.substringAfterLast('.')
    val originalFilePath = "${target.sourceFolderPath()}/src/main/java/$originalPackagePath/$originalClassName.kt"

    val originalAppId = target.getExtraOriginalApplicationId()
    val originalRelativeClassName = name.removePrefix(originalAppId)
    val originalFlattenComponentName = "$originalAppId/$originalRelativeClassName"

    val randomizedAppId = target.getExtraActualApplicationId()
    val randomizedPackagePath = randomizedAppId.replace('.', '/')
    val randomizedFileDirectoryPath = "${target.sourceFolderPath()}/src/main/java/$randomizedPackagePath"

    val randomClassName = random.nextString(10).uppercaseFirstChar()
    val randomFilePath = "$randomizedFileDirectoryPath/$randomClassName.kt"
    val randomizedFlattenComponentName = "$randomizedAppId/.$randomClassName"

    val placeholderKey = name.replaceFirstChar { it.lowercase() }
    val placeholderValue = "$randomizedAppId.$randomClassName"

    return ObfuscatedComponent(
        originalComponentName = name,
        originalPackageName = originalPackageName,
        originalFlattenComponentName = originalFlattenComponentName,
        originalClassName = originalClassName,
        originalFilePath = originalFilePath,
        randomizedClassName = randomClassName,
        randomizedFlattenComponentName = randomizedFlattenComponentName,
        randomizedFileDirectoryPath = randomizedFileDirectoryPath,
        randomizedFilePath = randomFilePath,
        manifestPlaceholderKey = placeholderKey,
        manifestPlaceholderValue = placeholderValue,
    )
}

