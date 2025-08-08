
package com.buzbuz.gradle.obfuscation.component

import com.buzbuz.gradle.obfuscation.extensions.ObfuscatedComponentNamedObject
import com.buzbuz.gradle.obfuscation.getExtraActualApplicationId
import com.buzbuz.gradle.obfuscation.getExtraOriginalApplicationId
import com.buzbuz.gradle.obfuscation.nextString
import com.buzbuz.gradle.obfuscation.sourceFolderPath
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

    val placeholderKey = originalClassName.replaceFirstChar { it.lowercase() }
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

