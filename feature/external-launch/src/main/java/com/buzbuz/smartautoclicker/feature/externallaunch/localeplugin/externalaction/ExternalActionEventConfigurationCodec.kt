/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.externalaction

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ExternalActionEventConfigurationCodec @Inject constructor() {

    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
    }

    fun encode(configuration: ExternalActionEventConfiguration): String {
        val normalized = configuration.normalized()
        require(normalized.isValid())
        return json.encodeToString(normalized)
    }

    fun decode(value: String?): ExternalActionEventConfiguration? {
        if (value.isNullOrBlank()) return null

        return runCatching {
            json.decodeFromString<ExternalActionEventConfiguration>(value)
                .normalized()
                .takeIf { it.isValid() }
        }.getOrNull()
    }
}
