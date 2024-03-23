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
package com.buzbuz.gradle.sourcedl

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

import java.io.FileOutputStream
import java.net.URL

abstract class DownloadZipTask : DefaultTask() {

    @get:Input
    abstract val projectAccount: Property<String>
    @get:Input
    abstract val projectName: Property<String>
    @get:Input
    abstract val projectVersion: Property<String>

    @get:OutputFile
    abstract val outputFile: Property<File>

    @TaskAction
    fun download() {
        val zipFile = outputFile.get()
        if (zipFile.exists()) {
            project.delete(zipFile)
        }
        project.mkdir(zipFile.parentFile)

        getUrlFromInputs().openStream().use {
            it.copyTo(FileOutputStream(zipFile))
        }
    }

    private fun getUrlFromInputs(): URL =
        URL("https://github.com/${projectAccount.get()}/${projectName.get()}/archive/refs/tags/${projectVersion.get()}.zip")
}