/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain

import kotlinx.serialization.Serializable

@Serializable
internal enum class LocalePluginOperation { LAUNCH, RUN_CURRENT, STOP }

@Serializable
internal data class LocalePluginConfiguration(
    val version: Int = CURRENT_VERSION,
    val operation: LocalePluginOperation,
    val scenarioId: Long? = null,
    val isSmart: Boolean? = null,
) {
    fun isValid(): Boolean = when (operation) {
        LocalePluginOperation.LAUNCH -> scenarioId != null && scenarioId > 0L && isSmart != null
        LocalePluginOperation.RUN_CURRENT,
        LocalePluginOperation.STOP -> scenarioId == null && isSmart == null
    }
}

@Serializable
internal data class SignedLocalePluginConfiguration(
    val version: Int,
    val operation: LocalePluginOperation,
    val scenarioId: Long? = null,
    val isSmart: Boolean? = null,
    val signature: String,
)

internal const val CURRENT_VERSION = 1
