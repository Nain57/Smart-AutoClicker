/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.database.utils

import android.database.Cursor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * Verify that the content of two lists are the same, in any order.
 *
 * @param T the type of the items in the lists.
 * @param expectedItems the list of expected items.
 * @param actualItems the list of actual items.
 * @param identifierProvider callback providing the identifier to match the items from actual and expected lists
 *                           before verifying their equality.
 */
fun <T> assertSameContent(expectedItems: List<T>, actualItems: List<T>, identifierProvider: (T) -> Long) {
    actualItems.forEach { actualItem ->
        val matchedExpected = expectedItems.find { expectedItem ->
            identifierProvider(expectedItem) == identifierProvider(actualItem)
        }

        assertEquals(matchedExpected, actualItem)
    }
}

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

fun Cursor.assertColumnEquals(expected: Boolean?, actualColumn: String) =
    if (expected == null) assertColumnNull(actualColumn)
    else assertEquals(
        "Invalid column value for $actualColumn",
        expected,
        getInt(getColumnIndex(actualColumn)) == 1,
    )

fun <T : Enum<T>> Cursor.assertColumnEquals(expected: T?, actualColumn: String) =
    if (expected == null) assertColumnNull(actualColumn)
    else assertEquals(
        "Invalid column value for $actualColumn",
        expected.toString(),
        getString(getColumnIndex(actualColumn)),
    )

fun Cursor.assertColumnNull(actualColumn: String) =
    assertTrue(
        "Invalid column value for $actualColumn",
        isNull(getColumnIndex(actualColumn)),
    )