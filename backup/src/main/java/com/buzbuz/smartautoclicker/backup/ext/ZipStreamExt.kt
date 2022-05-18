/*
 * Copyright (C) 2022 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.backup.ext

import java.io.File
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Write a file into the zip stream.
 * @param entry the file to be zipped.
 */
internal fun ZipOutputStream.writeEntryFile(entry: File) {
    entry.inputStream().use { input ->
        input.copyTo(this)
    }
}

/**
 * Read a file into from this zip stream.
 * @param output the file to put the content into.
 */
internal fun ZipInputStream.readEntryFile(output: File) =
    output.apply {
        outputStream().use { output ->
            copyTo(output)
        }
    }