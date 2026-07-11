/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.common.permissions.PermissionsController
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionAccessibilityService
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionOverlay
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.feature.externallaunch.domain.ExternalLaunchRepository
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginActionExecutor
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginConfigurationCodec
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.ResolvedLocalePluginAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class LocalePluginExecutionViewModel @Inject constructor(
    private val codec: LocalePluginConfigurationCodec,
    private val executor: LocalePluginActionExecutor,
    private val permissionController: PermissionsController,
    private val externalLaunchRepository: ExternalLaunchRepository,
    private val appComponentsProvider: AppComponentsProvider,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    fun resolve(configurationJson: String?, onResult: (ResolvedLocalePluginAction?) -> Unit) {
        val configuration = codec.decode(configurationJson)
        if (configuration == null) {
            onResult(null)
            return
        }
        viewModelScope.launch { onResult(executor.resolve(configuration)) }
    }

    fun requestPermissions(
        activity: AppCompatActivity,
        onAllGranted: () -> Unit,
        onMandatoryDenied: () -> Unit,
    ) {
        permissionController.startPermissionsUiFlow(
            activity = activity,
            permissions = listOf(
                PermissionOverlay(),
                PermissionAccessibilityService(
                    componentName = appComponentsProvider.klickrServiceComponentName,
                    isServiceRunning = { externalLaunchRepository.isAccessibilityServiceStarted() },
                ),
            ),
            onAllGranted = onAllGranted,
            onMandatoryDenied = onMandatoryDenied,
        )
    }

    fun launchDumb(action: ResolvedLocalePluginAction.LaunchDumb) = executor.launchDumb(action)

    fun launchSmart(resultCode: Int, data: Intent, action: ResolvedLocalePluginAction.LaunchSmart) =
        executor.launchSmart(resultCode, data, action)

    fun executeStop() = executor.executeStop()

    fun isEntireScreenCaptureForced(): Boolean = settingsRepository.isEntireScreenCaptureForced()
}
