
package com.buzbuz.smartautoclicker.feature.smart.config.domain.model

import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

data class EditedScenarioState(
    val scenario: Scenario,
    val imageEvents: List<Event>,
    val triggerEvents: List<Event>,
)