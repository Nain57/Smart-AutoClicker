/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.scenariostate

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.feature.externallaunch.domain.ExternalLaunchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ScenarioStateProvider @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val smartRepository: IRepository,
    private val dumbRepository: DumbRepository,
    private val externalLaunchRepository: ExternalLaunchRepository,
) {
    suspend fun getSnapshot(): ScenarioStateSnapshot = withContext(ioDispatcher) {
        val smartScenarioId = externalLaunchRepository.getSmartScenarioId()
        val dumbScenarioId = if (smartScenarioId == null) externalLaunchRepository.getDumbScenarioId() else null
        val scenarioId = smartScenarioId ?: dumbScenarioId
        val scenarioName = when {
            smartScenarioId != null -> smartRepository.getScenario(smartScenarioId)?.name.orEmpty()
            dumbScenarioId != null -> dumbRepository.getDumbScenario(dumbScenarioId)?.name.orEmpty()
            else -> ""
        }

        ScenarioStateSnapshot(
            scenarioName = scenarioName,
            state = ScenarioState.from(
                isScenarioOpen = scenarioId != null,
                isRunning = externalLaunchRepository.isScenarioRunning(),
                isOverlayHidden = externalLaunchRepository.isOverlayHidden(),
                isSettingsOpen = externalLaunchRepository.isScenarioConfigurationOpen(),
            ),
        )
    }
}
