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

import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.*

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Test the [JsonObject] extensions method.. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class JsonObjectExtTests {

    /** Enum class used as test data. */
    @Suppress("unused")
    private enum class TestEnum {
        TEST_VALUE_1,
        TEST_VALUE_2,
        TEST_VALUE_3,
    }

    @Test
    fun getJsonObject_presentAndValid() {
        val resultKey = "TOTO"
        val resultJsonObject = JsonObject(emptyMap())
        val testedObject = JsonObject(mapOf(resultKey to resultJsonObject))

        assertEquals(resultJsonObject, testedObject.getJsonObject(resultKey))
    }

    @Test
    fun getJsonObject_presentAndInvalid() {
        val resultKey = "TOTO"
        val invalidResult = JsonPrimitive(3)
        val testedObject = JsonObject(mapOf(resultKey to invalidResult))

        assertEquals(null, testedObject.getJsonObject(resultKey))
    }

    @Test
    fun getJsonObject_absent() {
        val testedObject = JsonObject(emptyMap())

        assertEquals(null, testedObject.getJsonObject("TOTO"))
    }

    @Test
    fun getJsonArray_presentAndValid() {
        val resultKey = "TOTO"
        val resultJsonArray = JsonArray(listOf(JsonObject(emptyMap())))
        val testedObject = JsonObject(mapOf(resultKey to resultJsonArray))

        assertEquals(resultJsonArray, testedObject.getJsonArray(resultKey))
    }

    @Test
    fun getJsonArray_presentAndInvalid() {
        val resultKey = "TOTO"
        val invalidResult = JsonPrimitive(3)
        val testedObject = JsonObject(mapOf(resultKey to invalidResult))

        assertEquals(null, testedObject.getJsonArray(resultKey))
    }

    @Test
    fun getJsonArray_absent() {
        val testedObject = JsonObject(emptyMap())

        assertEquals(null, testedObject.getJsonArray("TOTO"))
    }

    @Test
    fun getBoolean_presentAndValid() {
        val resultKey = "TOTO"
        val resultBoolean = JsonPrimitive(true)
        val testedObject = JsonObject(mapOf(resultKey to resultBoolean))

        assertEquals(resultBoolean.boolean, testedObject.getBoolean(resultKey))
    }

    @Test
    fun getBoolean_presentAndInvalid() {
        val resultKey = "TOTO"
        val invalidResult = JsonPrimitive(3)
        val testedObject = JsonObject(mapOf(resultKey to invalidResult))

        assertEquals(null, testedObject.getBoolean(resultKey))
    }

    @Test
    fun getBoolean_absent() {
        val testedObject = JsonObject(emptyMap())

        assertEquals(null, testedObject.getBoolean("TOTO"))
    }

    @Test
    fun getInt_presentAndValid() {
        val resultKey = "TOTO"
        val resultInt = JsonPrimitive(3)
        val testedObject = JsonObject(mapOf(resultKey to resultInt))

        assertEquals(resultInt.int, testedObject.getInt(resultKey))
    }

    @Test
    fun getInt_presentAndInvalid() {
        val resultKey = "TOTO"
        val invalidResult = JsonPrimitive(true)
        val testedObject = JsonObject(mapOf(resultKey to invalidResult))

        assertEquals(null, testedObject.getInt(resultKey))
    }

    @Test
    fun getInt_absent() {
        val testedObject = JsonObject(emptyMap())

        assertEquals(null, testedObject.getInt("TOTO"))
    }

    @Test
    fun getLong_presentAndValid() {
        val resultKey = "TOTO"
        val resultLong = JsonPrimitive(3L)
        val testedObject = JsonObject(mapOf(resultKey to resultLong))

        assertEquals(resultLong.long, testedObject.getLong(resultKey))
    }

    @Test
    fun getLong_presentAndInvalid() {
        val resultKey = "TOTO"
        val invalidResult = JsonPrimitive(true)
        val testedObject = JsonObject(mapOf(resultKey to invalidResult))

        assertEquals(null, testedObject.getLong(resultKey))
    }

    @Test
    fun getLong_absent() {
        val testedObject = JsonObject(emptyMap())

        assertEquals(null, testedObject.getLong("TOTO"))
    }

    @Test
    fun getString_presentAndValid() {
        val resultKey = "TOTO"
        val resultString = "TUTU"
        val testedObject = JsonObject(mapOf(resultKey to JsonPrimitive("TUTU")))

        assertEquals(resultString, testedObject.getString(resultKey))
    }

    @Test
    fun getString_presentAndInvalid() {
        val resultKey = "TOTO"
        val invalidResult = JsonPrimitive(3)
        val testedObject = JsonObject(mapOf(resultKey to invalidResult))

        assertEquals(null, testedObject.getString(resultKey))
    }

    @Test
    fun getString_absent() {
        val testedObject = JsonObject(emptyMap())

        assertEquals(null, testedObject.getString("TOTO"))
    }

    @Test
    fun getEnum_presentAndValid() {
        val resultKey = "TOTO"
        val resultString = JsonPrimitive(TestEnum.TEST_VALUE_1.name)
        val testedObject = JsonObject(mapOf(resultKey to resultString))

        assertEquals(TestEnum.TEST_VALUE_1, testedObject.getEnum<TestEnum>(resultKey))
    }

    @Test
    fun getEnum_presentAndInvalidString() {
        val resultKey = "TOTO"
        val invalidResult = JsonPrimitive("grsblfx")
        val testedObject = JsonObject(mapOf(resultKey to invalidResult))

        assertEquals(null, testedObject.getEnum<TestEnum>(resultKey))
    }

    @Test
    fun getEnum_presentAndInvalidNumber() {
        val resultKey = "TOTO"
        val invalidResult = JsonPrimitive(3)
        val testedObject = JsonObject(mapOf(resultKey to invalidResult))

        assertEquals(null, testedObject.getEnum<TestEnum>(resultKey))
    }

    @Test
    fun getEnum_absent() {
        val testedObject = JsonObject(emptyMap())

        assertEquals(null, testedObject.getEnum<TestEnum>("TOTO"))
    }
}