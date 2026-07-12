/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.externalaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalActionEventConfigurationCodecTest {

    private val codec = ExternalActionEventConfigurationCodec()

    @Test
    fun encodeDecode_validConfiguration() {
        val decoded = codec.decode(
            codec.encode(ExternalActionEventConfiguration(externalActionName = "Open xyz game intent"))
        )

        assertEquals(
            ExternalActionEventConfiguration(externalActionName = "Open xyz game intent"),
            decoded,
        )
    }

    @Test
    fun encodeDecode_trimmedName() {
        val decoded = codec.decode(
            codec.encode(ExternalActionEventConfiguration(externalActionName = "  Open xyz game intent  "))
        )

        assertEquals("Open xyz game intent", decoded?.externalActionName)
    }

    @Test
    fun decode_rejectsEmptyName() {
        assertNull(codec.decode("""{"version":1,"externalActionName":"   "}"""))
    }

    @Test
    fun decode_rejectsUnknownVersion() {
        assertNull(codec.decode("""{"version":2,"externalActionName":"Open xyz game intent"}"""))
    }

    @Test
    fun decode_rejectsMissingField() {
        assertNull(codec.decode("""{"version":1}"""))
    }
}
