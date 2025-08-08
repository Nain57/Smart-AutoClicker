
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

