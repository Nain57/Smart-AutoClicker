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
package com.buzbuz.smartautoclicker

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register
import java.io.File

class GitHubFetchPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val extension = target.extensions.create<GithubFetchPluginExtension>(PLUGIN_EXTENSION_NAME)

        target.afterEvaluate {
            extension.projects.forEach { gitHubProject ->
                val taskNameSuffix = gitHubProject.name.toTaskNameSuffix()

                val downloadZipTask = target.registerDownloadTask(gitHubProject, taskNameSuffix)
                val extractZipTask = target.registerExtractTask(downloadZipTask, gitHubProject, taskNameSuffix)

                gitHubProject.fetchForTask.get().dependsOn(extractZipTask)
            }
        }
    }

    private fun Project.registerDownloadTask(
        gitHubProject: GitHubProject,
        taskNameSuffix: String,
    ): TaskProvider<GitHubDownloadZipTask> =
        tasks.register<GitHubDownloadZipTask>(TASK_NAME_DOWNLOAD_ZIP + taskNameSuffix) {
            group = TASKS_GROUP_NAME

            projectAccount.set(gitHubProject.projectAccount)
            projectName.set(gitHubProject.projectName)
            projectVersion.set(gitHubProject.projectVersion)
            outputFile.set(
                layout.buildDirectory.dir("$PLUGIN_INTERMEDIATES_FOLDER/$TASK_NAME_DOWNLOAD_ZIP").map { directory ->
                    File(directory.asFile, gitHubProject.getSourceZipFileName().get())
                }
            )
        }

    private fun Project.registerExtractTask(
        downloadTask: TaskProvider<GitHubDownloadZipTask>,
        gitHubProject: GitHubProject,
        taskNameSuffix: String,
    ): TaskProvider<GitHubExtractZipTask> =
        tasks.register<GitHubExtractZipTask>(TASK_NAME_EXTRACT_ZIP + taskNameSuffix) {
            dependsOn(downloadTask)
            group = TASKS_GROUP_NAME

            inputZipFile.set(downloadTask.flatMap { it.outputFile })
            sourceVersion.set(gitHubProject.projectVersion)
            outputDirectory.set(gitHubProject.unzipPath)
        }

    private fun GitHubProject.getSourceZipFileName(): Provider<String> =
        projectName.zip(projectVersion) { name, version ->
            "$name-$version.zip"
        }

    private fun String.toTaskNameSuffix(): String =
        replaceFirstChar { it.uppercaseChar() }

    internal companion object {
        const val PLUGIN_EXTENSION_NAME = "gitHubFetch"
        const val PLUGIN_INTERMEDIATES_FOLDER = "intermediates/gitHubFetch"

        const val TASKS_GROUP_NAME = "github fetch"
        const val TASK_NAME_DOWNLOAD_ZIP = "downloadZip"
        const val TASK_NAME_EXTRACT_ZIP = "extractZip"
    }
}
