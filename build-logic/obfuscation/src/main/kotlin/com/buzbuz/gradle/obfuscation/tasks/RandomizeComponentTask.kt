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
package com.buzbuz.gradle.obfuscation.tasks

import com.buzbuz.gradle.obfuscation.component.ObfuscatedComponent
import com.buzbuz.gradle.obfuscation.getExtraActualApplicationId
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.io.File


internal fun Project.registerRandomizeTask(obfuscatedComponent: ObfuscatedComponent) : TaskProvider<Task> =
    tasks.register(obfuscatedComponent.getRandomizeTaskName()) {
        group = TASK_GROUP
        description = "Randomizes the Android component name before compilation."

        doLast { obfuscatedComponent.randomize(this@registerRandomizeTask) }
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