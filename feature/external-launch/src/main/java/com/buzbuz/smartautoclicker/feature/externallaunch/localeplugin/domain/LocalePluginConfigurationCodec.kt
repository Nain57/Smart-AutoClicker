/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalePluginConfigurationCodec @Inject constructor(
    private val signer: LocalePluginConfigurationSigner,
) {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = true
        ignoreUnknownKeys = false
    }

    fun encode(configuration: LocalePluginConfiguration): String {
        require(configuration.version == CURRENT_VERSION && configuration.isValid())
        val payload = json.encodeToString(configuration)
        return json.encodeToString(
            SignedLocalePluginConfiguration(
                version = configuration.version,
                operation = configuration.operation,
                scenarioId = configuration.scenarioId,
                isSmart = configuration.isSmart,
                signature = signer.sign(payload),
            )
        )
    }

    fun decode(value: String?): LocalePluginConfiguration? {
        if (value.isNullOrBlank()) return null

        return runCatching {
            val signed = json.decodeFromString<SignedLocalePluginConfiguration>(value)
            val configuration = LocalePluginConfiguration(
                version = signed.version,
                operation = signed.operation,
                scenarioId = signed.scenarioId,
                isSmart = signed.isSmart,
            )
            if (configuration.version != CURRENT_VERSION || !configuration.isValid()) return null

            val payload = json.encodeToString(configuration)
            configuration.takeIf { signer.verify(payload, signed.signature) }
        }.getOrNull()
    }
}
