/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.core.common.tutorial.impl.engine.step

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStepEndCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.impl.monitoring.MonitoredViewsManagerImpl

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class TutorialStepEndConditionMonitor @Inject constructor(
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val monitoredViewsManager: MonitoredViewsManagerImpl,
    private val overlayManager: OverlayManager,
) {

    private val coroutineScopeIo: CoroutineScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var stepConditionMonitoringJob: Job? = null


    fun monitorCondition(
        condition: TutorialStepEndCondition,
        onConditionReached: () -> Unit
    ) = when (condition) {

        is TutorialStepEndCondition.MonitoredViewClicked -> {
            monitoredViewsManager.monitorNextClick(
                type = condition.type,
                listener = onConditionReached,
            )
        }

        is TutorialStepEndCondition.OverlayStackVisibilityChanged -> {
            val expectedVisibility = condition.newVisibility
            stepConditionMonitoringJob = coroutineScopeIo.launch {
                overlayManager.isStackHidden.collect { isStackHidden ->
                    if (isStackHidden == expectedVisibility) return@collect

                    onConditionReached()
                    stepConditionMonitoringJob?.cancel()
                    stepConditionMonitoringJob = null
                }
            }
        }

        TutorialStepEndCondition.Immediate ->
            onConditionReached()

        TutorialStepEndCondition.NextButton -> Unit // handled via onNextStepButtonPressed
    }

    fun stopMonitoring(condition: TutorialStepEndCondition) {
        when (condition) {
            is TutorialStepEndCondition.MonitoredViewClicked -> {
                monitoredViewsManager.stopNextClickMonitoring(condition.type)
            }

            is TutorialStepEndCondition.OverlayStackVisibilityChanged -> {
                stepConditionMonitoringJob?.cancel()
                stepConditionMonitoringJob = null
            }

            TutorialStepEndCondition.NextButton,
            TutorialStepEndCondition.Immediate -> Unit
        }
    }

    fun clearMonitoring() {
        stepConditionMonitoringJob?.cancel()
        stepConditionMonitoringJob = null
    }
}