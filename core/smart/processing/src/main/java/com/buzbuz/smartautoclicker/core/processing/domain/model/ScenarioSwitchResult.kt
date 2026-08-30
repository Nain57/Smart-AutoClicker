/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.core.processing.domain.model

/** Result of attempting to change the Smart scenario loaded by the paused processor. */
sealed interface ScenarioSwitchResult {
    data object Success : ScenarioSwitchResult
    data object ServiceUnavailable : ScenarioSwitchResult
    data object InvalidProcessingState : ScenarioSwitchResult
    data object ProjectionUnavailable : ScenarioSwitchResult
    data object CurrentScenario : ScenarioSwitchResult
    data object ScenarioUnavailable : ScenarioSwitchResult
    data object PersistenceFailure : ScenarioSwitchResult
}
