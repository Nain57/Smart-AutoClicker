/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.common.permissions.PermissionsController
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginConfiguration
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginConfigurationCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class LocalePluginConfigurationViewModel @Inject constructor(
    smartRepository: IRepository,
    dumbRepository: DumbRepository,
    private val codec: LocalePluginConfigurationCodec,
    private val permissionController: PermissionsController,
) : ViewModel() {

    val scenarios: StateFlow<List<LocalePluginScenarioItem>> = combine(
        smartRepository.scenarios,
        dumbRepository.dumbScenarios,
    ) { smartScenarios, dumbScenarios ->
        (smartScenarios.map { scenario ->
            LocalePluginScenarioItem(scenario.id.databaseId, scenario.name, isSmart = true)
        } + dumbScenarios.map { scenario ->
            LocalePluginScenarioItem(scenario.id.databaseId, scenario.name, isSmart = false)
        }).sortedBy { it.name.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun decodeConfiguration(value: String?): LocalePluginConfiguration? = codec.decode(value)

    fun encodeConfiguration(configuration: LocalePluginConfiguration): String = codec.encode(configuration)

    fun requestFallbackNotificationPermission(activity: AppCompatActivity, onGranted: () -> Unit) {
        permissionController.startPermissionsUiFlow(
            activity = activity,
            permissions = listOf(
                PermissionPostNotification(
                    optional = true,
                    purpose = PermissionPostNotification.Purpose.EXTERNAL_LAUNCH_FALLBACK,
                ),
            ),
            onAllGranted = onGranted,
        )
    }

}

internal data class LocalePluginScenarioItem(
    val id: Long,
    val name: String,
    val isSmart: Boolean,
) {
    override fun toString(): String = name
}
