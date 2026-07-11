/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain

import android.content.Context
import android.content.Intent
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionOverlay
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.externallaunch.domain.ExternalLaunchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalePluginActionExecutor @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val smartRepository: IRepository,
    private val dumbRepository: DumbRepository,
    private val externalLaunchRepository: ExternalLaunchRepository,
) {
    suspend fun resolve(configuration: LocalePluginConfiguration): ResolvedLocalePluginAction? =
        withContext(ioDispatcher) {
            when (configuration.operation) {
                LocalePluginOperation.STOP -> ResolvedLocalePluginAction.Stop
                LocalePluginOperation.LAUNCH -> {
                    val id = configuration.scenarioId ?: return@withContext null
                    if (configuration.isSmart == true) {
                        smartRepository.getScenario(id)?.let(ResolvedLocalePluginAction::LaunchSmart)
                    } else {
                        dumbRepository.getDumbScenario(id)?.let(ResolvedLocalePluginAction::LaunchDumb)
                    }
                }
            }
        }

    fun areBasePermissionsReady(context: Context): Boolean =
        PermissionOverlay().checkIfGranted(context) && externalLaunchRepository.isAccessibilityServiceStarted()

    fun isScenarioConfigurationOpen(): Boolean = externalLaunchRepository.isScenarioConfigurationOpen()

    fun executeStop() = externalLaunchRepository.stopScenarios()

    fun launchDumb(action: ResolvedLocalePluginAction.LaunchDumb) {
        if (externalLaunchRepository.isDumbScenarioRunning(action.scenario.id.databaseId)) return
        externalLaunchRepository.replaceDumbScenario(action.scenario)
    }

    fun launchSmart(resultCode: Int, data: Intent, action: ResolvedLocalePluginAction.LaunchSmart) =
        externalLaunchRepository.replaceSmartScenario(resultCode, data, action.scenario)

    fun launchSmartWithCurrentProjection(action: ResolvedLocalePluginAction.LaunchSmart): Boolean {
        if (!externalLaunchRepository.isSmartScreenRecordActive()) return false

        val currentScenarioId = externalLaunchRepository.getSmartScenarioId()
        if (currentScenarioId == action.scenario.id.databaseId &&
            externalLaunchRepository.isAccessibilityServiceStarted()
        ) {
            return true
        }

        externalLaunchRepository.replaceSmartScenarioWithCurrentProjection(action.scenario)
        return true
    }
}

internal sealed interface ResolvedLocalePluginAction {
    data object Stop : ResolvedLocalePluginAction
    data class LaunchSmart(val scenario: Scenario) : ResolvedLocalePluginAction
    data class LaunchDumb(val scenario: DumbScenario) : ResolvedLocalePluginAction

    val scenarioName: String?
        get() = when (this) {
            Stop -> null
            is LaunchSmart -> scenario.name
            is LaunchDumb -> scenario.name
        }
}
