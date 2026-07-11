/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.externalaction

import kotlinx.serialization.Serializable

internal const val EXTERNAL_ACTION_EVENT_CONFIG_VERSION = 1

@Serializable
internal data class ExternalActionEventConfiguration(
    val version: Int = EXTERNAL_ACTION_EVENT_CONFIG_VERSION,
    val externalActionName: String,
) {
    fun normalized(): ExternalActionEventConfiguration =
        copy(externalActionName = externalActionName.trim())

    fun isValid(): Boolean =
        version == EXTERNAL_ACTION_EVENT_CONFIG_VERSION && externalActionName.trim().isNotEmpty()
}
