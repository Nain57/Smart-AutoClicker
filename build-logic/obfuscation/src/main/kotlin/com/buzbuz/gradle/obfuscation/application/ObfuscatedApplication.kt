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
package com.buzbuz.gradle.obfuscation.application

import com.buzbuz.gradle.obfuscation.extensions.ObfuscatedApplicationNamedObject
import com.buzbuz.gradle.obfuscation.getExtraActualApplicationId
import com.buzbuz.gradle.obfuscation.nextString
import com.buzbuz.gradle.obfuscation.sourceFolderPath
import org.gradle.api.Project
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import kotlin.random.Random


internal data class ObfuscatedApplication(
    val originalFullClassName: String,
    val originalPackageName: String,
    val originalClassName: String,
    val originalFilePath: String,
    val randomizedPackageName: String,
    val randomizedClassName: String,
    val randomizedFileDirectoryPath: String,
    val randomizedFilePath: String,
    val manifestPlaceholderKey: String,
    val manifestPlaceholderValue: String,
    val tempFilePath: String,
    val tempFileName: String,
)

internal fun ObfuscatedApplicationNamedObject.toDomain(target: Project, random: Random): ObfuscatedApplication {
    val originalPackagePath = name.substringBeforeLast('.').replace('.', '/')
    val originalPackageName = name.substringBeforeLast('.')
    val originalClassName = name.substringAfterLast('.')
    val originalFilePath = "${target.sourceFolderPath()}/src/main/java/$originalPackagePath/$originalClassName.kt"

    val randomizedAppId = target.getExtraActualApplicationId()
    val randomizedPackagePath = randomizedAppId.replace('.', '/')
    val randomizedFileDirectoryPath = "${target.sourceFolderPath()}/src/main/java/$randomizedPackagePath"

    val randomClassName = random.nextString(10).uppercaseFirstChar()
    val randomFilePath = "$randomizedFileDirectoryPath/$randomClassName.kt"

    val placeholderKey = originalClassName.replaceFirstChar { it.lowercase() }
    val placeholderValue = "$randomizedAppId.$randomClassName"

    val tempFilePath = originalFilePath.replace(".kt", ".tmp")
    val tempFileName = originalClassName.substringAfterLast('.')

    return ObfuscatedApplication(
        originalFullClassName = name,
        originalPackageName = originalPackageName,
        originalClassName = originalClassName,
        originalFilePath = originalFilePath,
        randomizedPackageName = randomizedAppId,
        randomizedClassName = randomClassName,
        randomizedFileDirectoryPath = randomizedFileDirectoryPath,
        randomizedFilePath = randomFilePath,
        manifestPlaceholderKey = placeholderKey,
        manifestPlaceholderValue = placeholderValue,
        tempFilePath = tempFilePath,
        tempFileName = tempFileName,
    )
}

