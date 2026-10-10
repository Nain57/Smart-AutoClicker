/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.dumb.utils

import android.database.Cursor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/** Check the number of items in a cursor. */
fun Cursor.assertCountEquals(expectedCount: Int) =
    assertEquals("Invalid list size", expectedCount, count)

fun Cursor.assertColumnEquals(expected: Int?, actualColumn: String) =
    if (expected == null) assertColumnNull(actualColumn)
    else assertEquals(
        "Invalid column value for $actualColumn",
        expected,
        getInt(getColumnIndex(actualColumn))
    )

fun Cursor.assertColumnEquals(expected: Long?, actualColumn: String) =
    if (expected == null) assertColumnNull(actualColumn)
    else assertEquals(
        "Invalid column value for $actualColumn",
        expected,
        getLong(getColumnIndex(actualColumn))
    )

fun Cursor.assertColumnEquals(expected: String?, actualColumn: String) =
    if (expected == null) assertColumnNull(actualColumn)
    else assertEquals(
        "Invalid column value for $actualColumn",
        expected,
        getString(getColumnIndex(actualColumn))
    )

fun Cursor.assertColumnNull(actualColumn: String) =
    assertTrue(
        "Invalid column value for $actualColumn",
        isNull(getColumnIndex(actualColumn)),
    )

fun Cursor.assertColumnMissing(column: String) =
    assertEquals("Column $column should not exist", -1, getColumnIndex(column))
