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
package com.buzbuz.smartautoclicker.core.base.extensions

import android.util.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

fun File.safeRecreate() {
    safeDelete()
    safeCreate()
}

fun File.safeCreate() {
    if (safeExists()) return

    try {
        createNewFile()
    } catch (sEx: SecurityException) {
        Log.e(LOG_TAG, "Cannot create file $name, permission denied", sEx)
    } catch (ioEx: IOException) {
        Log.e(LOG_TAG, "Cannot create file $name, IOException", ioEx)
    }
}

fun File.safeDelete() {
    if (!safeExists()) return

    try {
        delete()
    } catch (sEx: SecurityException) {
        Log.e(LOG_TAG, "Cannot delete file $name, permission denied", sEx)
    } catch (ioEx: IOException) {
        Log.e(LOG_TAG, "Cannot delete file $name, IOException", ioEx)
    }
}

fun File.safeBufferedOutputStream(append: Boolean = true): BufferedOutputStream? {
    if (!safeExists()) return null

    try {
        return FileOutputStream(this, append).buffered()
    } catch (sEx: SecurityException) {
        Log.e(LOG_TAG, "Cannot open file $name, permission denied", sEx)
    } catch (fnfEx: FileNotFoundException) {
        Log.e(LOG_TAG, "Cannot open file $name, file is not found", fnfEx)
    }

    return null
}

fun File.safeInputStream(): FileInputStream? {
    if (!safeExists()) return null

    try {
        return FileInputStream(this)
    } catch (sEx: SecurityException) {
        Log.e(LOG_TAG, "Cannot open file $name, permission denied", sEx)
    } catch (fnfEx: FileNotFoundException) {
        Log.e(LOG_TAG, "Cannot open file $name, file is not found", fnfEx)
    }

    return null
}

fun File.safeExists(): Boolean =
    try {
        exists()
    } catch (sEx: SecurityException) {
        Log.e(LOG_TAG, "Cannot check if file $name exists, permission denied", sEx)
        false
    }

private const val LOG_TAG = "FileExt"