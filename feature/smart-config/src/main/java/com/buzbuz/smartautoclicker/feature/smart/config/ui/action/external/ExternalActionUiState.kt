/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.smart.config.ui.action.external

data class ExternalActionUiState(
    val canBeSaved: Boolean,
    val hasUnsavedModifications: Boolean,
    val name: String?,
    val nameError: Boolean,
    val externalActionName: String,
    val externalActionNameError: Boolean,
)
