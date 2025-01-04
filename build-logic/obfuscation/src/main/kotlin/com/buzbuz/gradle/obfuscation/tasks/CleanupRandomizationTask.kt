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

import com.buzbuz.gradle.obfuscation.application.ObfuscatedApplication
import com.buzbuz.gradle.obfuscation.component.ObfuscatedComponent
import com.buzbuz.gradle.obfuscation.getExtraActualApplicationId
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.io.File


internal fun Project.registerCleanupTask(obfuscatedApplication: ObfuscatedApplication) : TaskProvider<Task> =
    tasks.register(TASK_NAME_CLEAN_APPLICATION) {
        group = TASK_GROUP
        description = "Removes the randomized Android Application class file after compilation."

        doLast { obfuscatedApplication.cleanup(this@registerCleanupTask) }
    }

private fun ObfuscatedApplication.cleanup(target: Project) {
    val tmpFile = File(tempFilePath)
    if (tmpFile.exists()) {
        tmpFile.renameTo(File(originalFilePath))
    }

    val file = File(randomizedFilePath)
    if (!file.exists()) return

    file.delete()
    file.deletePackageFolderIfLast(target)
}

internal fun Project.registerCleanupTask(obfuscatedComponent: ObfuscatedComponent) : TaskProvider<Task> =
    tasks.register(obfuscatedComponent.getCleanTaskName()) {
        group = TASK_GROUP
        description = "Removes the randomized Android component files after compilation."

        doLast { obfuscatedComponent.cleanup(this@registerCleanupTask) }
    }

private fun ObfuscatedComponent.cleanup(target: Project) {
    val file = File(randomizedFilePath)
    if (!file.exists()) return

    file.delete()
    file.deletePackageFolderIfLast(target)
}

private fun File.deletePackageFolderIfLast(target: Project) {
    if (parentFile.listFiles()?.isEmpty() == false) return

    val sourceFolderPath = target.layout.projectDirectory.toString()
    val randomizedPackagePath = target.getExtraActualApplicationId()
        .split('.').first()

    val packageFolder = File("$sourceFolderPath/src/main/java/$randomizedPackagePath")
    if (packageFolder.exists()) {
        packageFolder.deleteRecursively()
    }
}

private const val TASK_NAME_CLEAN_APPLICATION = "cleanRandomizedApplication"
private fun ObfuscatedComponent.getCleanTaskName(): String =
    "clean${originalComponentName.split(".").last().uppercaseFirstChar()}Class"