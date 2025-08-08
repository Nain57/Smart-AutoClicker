
package com.buzbuz.gradle.obfuscation.tasks

import com.buzbuz.gradle.obfuscation.component.ObfuscatedComponent
import com.buzbuz.gradle.obfuscation.getExtraActualApplicationId
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.io.File


internal fun Project.registerRandomizeComponentTask(obfuscatedComponent: ObfuscatedComponent) : TaskProvider<Task> =
    tasks.register(obfuscatedComponent.getRandomizeTaskName()) {
        group = TASK_GROUP
        description = "Randomizes the Android component name before compilation."

        doLast { obfuscatedComponent.randomize(this@registerRandomizeComponentTask) }
    }


private fun ObfuscatedComponent.randomize(target: Project) {
    val originalFile = File(originalFilePath)
    if (!originalFile.exists())
        throw IllegalArgumentException("Cannot locate original component $originalClassName")

    // Create the directory structure for the randomized package
    val newDirectory = File(randomizedFileDirectoryPath)
    if (!newDirectory.exists()) {
        newDirectory.mkdirs()
    }

    // Move the class file to the new package
    val newFile = File(randomizedFilePath)
    originalFile.copyTo(newFile, overwrite = true)
    if (!newFile.exists())
        throw IllegalArgumentException("Cannot create new file $newFile")

    // Read the content of the file
    val fileContent = newFile.readText()

    // Replace the package declaration and the class name
    val updatedContent = fileContent
        .replace(Regex("package\\s+$originalPackageName"), "package ${target.getExtraActualApplicationId()}")
        .replace("\\b$originalClassName\\b".toRegex(), randomizedClassName) // Replace old class name with the new one

    // Write the updated content back to the file
    newFile.writeText(updatedContent)
}

private fun ObfuscatedComponent.getRandomizeTaskName(): String =
    "randomize${originalComponentName.split(".").last().uppercaseFirstChar()}Class"