
package com.buzbuz.gradle.obfuscation.tasks

import com.buzbuz.gradle.obfuscation.application.ObfuscatedApplication
import com.buzbuz.gradle.obfuscation.getExtraActualApplicationId
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import java.io.File

internal fun Project.registerRandomizeApplicationTask(obfuscatedApplication: ObfuscatedApplication) : TaskProvider<Task> =
    tasks.register(TASK_NAME) {
        group = TASK_GROUP
        description = "Randomizes the Android Application class name before compilation."

        doLast { obfuscatedApplication.randomize(this@registerRandomizeApplicationTask) }
    }


private fun ObfuscatedApplication.randomize(target: Project) {
    val originalFile = File(originalFilePath)
    if (!originalFile.exists())
        throw IllegalArgumentException("Cannot locate original application $originalClassName at $originalFilePath")

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

    originalFile.renameTo(File(tempFilePath))

    // Read the content of the file
    val fileContent = newFile.readText()

    // Replace the package declaration and the class name
    val updatedContent = fileContent
        .replace(Regex("package\\s+$originalPackageName"), "package ${target.getExtraActualApplicationId()}")
        .replace("\\b$originalClassName\\b".toRegex(), randomizedClassName) // Replace old class name with the new one

    // Write the updated content back to the file
    newFile.writeText(updatedContent)
}

private const val TASK_NAME = "obfuscateApplicationClass"