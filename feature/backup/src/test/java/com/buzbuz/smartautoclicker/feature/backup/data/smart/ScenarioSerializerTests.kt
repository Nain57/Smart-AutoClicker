/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.backup.data.smart

import android.os.Build

import com.buzbuz.smartautoclicker.core.database.DATABASE_VERSION
import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Regression coverage for forward-compatible backup imports. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class ScenarioSerializerTests {

    private val serializer = ScenarioSerializer()

    @Test
    fun deserialize_futureScenarioField_preservesKnownScenarioData() {
        val output = ByteArrayOutputStream()
        serializer.serialize(
            ScenarioBackup(
                version = DATABASE_VERSION,
                screenWidth = 1080,
                screenHeight = 1920,
                scenario = CompleteScenario(
                    scenario = ScenarioEntity(1L, "Scenario", 600),
                    events = emptyList(),
                    counters = emptyList(),
                ),
            ),
            output,
        )
        val originalBackup = Json.parseToJsonElement(output.toString(Charsets.UTF_8)).jsonObject
        val scenario = originalBackup.getValue("scenario").jsonObject
        val backupWithFutureField = JsonObject(
            originalBackup + ("scenario" to JsonObject(scenario + ("futureField" to JsonPrimitive(true)))),
        ).toString()

        val result = serializer.deserialize(backupWithFutureField.asInputStream())

        assertNotNull(result)
        assertEquals("Scenario", result?.scenario?.scenario?.name)
    }

    private fun String.asInputStream() = ByteArrayInputStream(toByteArray())
}
