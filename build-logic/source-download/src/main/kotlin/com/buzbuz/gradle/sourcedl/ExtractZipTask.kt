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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

abstract class ExtractZipTask : DefaultTask() {

    @get:Input
    abstract val sourceVersion: Property<String>
    @get:Input
    abstract val inputZipFile: Property<File>
    @get:Input
    abstract val fileFiltersRegexes: ListProperty<String>
    @get:Input
    abstract val foldersMapping: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun extract() {
        val outputDir = outputDirectory.get()

        val sourceCodeVersionFile = project.file("${outputDir.asFile.path}/version.txt")
        if (sourceCodeVersionFile.exists()) {
            if (sourceCodeVersionFile.readText() != sourceVersion.get()) {
                project.delete(outputDir)
            } else {
                return
            }
        }
        project.mkdir(outputDir)


        val filters = fileFiltersRegexes.get().map { Regex(it) }
        val mapping = foldersMapping.get()

        ZipFile(inputZipFile.get()).use { zipFile ->
            zipFile.entries().asSequence().forEach { entry ->
                zipFile.copyZipEntryIfNeeded(entry, outputDir.asFile, filters, mapping)
            }
        }

        project.file(sourceCodeVersionFile.toPath())
            .writeText(sourceVersion.get())
    }

    private fun ZipFile.copyZipEntryIfNeeded(entry: ZipEntry, outputDir: File, filters: List<Regex>, mapping: Map<String, String>) {
        if (entry.isDirectory) return
        var entryPath = entry.name.split("/").drop(1).joinToString("/")

        // Filters unwanted files
        if (filters.isNotEmpty() && !entryPath.matchFilters(filters)) return

        // Map to new folder if needed
        entryPath.getMapping(mapping)?.let { (toReplace, replacement) ->
            entryPath = entryPath.replace(toReplace, replacement)
        }

        File("${outputDir.path}/$entryPath").let { outputFile ->
            project.mkdir(outputFile.parentFile.toPath())
            getInputStream(entry).use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun String.matchFilters(filters: List<Regex>): Boolean =
        filters.find { filter -> matches(filter) } != null

    private fun String.getMapping(mapping: Map<String, String>): Pair<String, String>? {
        mapping.forEach { (toReplace, replacement) ->
            if (startsWith(toReplace)) return toReplace to replacement
        }

        return null
    }
}