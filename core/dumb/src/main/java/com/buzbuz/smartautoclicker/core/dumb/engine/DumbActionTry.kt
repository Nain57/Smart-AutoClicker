
package com.buzbuz.smartautoclicker.core.dumb.engine

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction.DumbClick
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction.DumbPause
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction.DumbSwipe
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

internal fun DumbAction.toDumbScenarioTry(): DumbScenario {
    val scenarioId = Identifier(databaseId = 1L)

    return DumbScenario(
        id = scenarioId,
        name = "Try",
        repeatCount = 1,
        isRepeatInfinite = false,
        maxDurationMin = 1,
        isDurationInfinite = false,
        randomize = false,
        dumbActions = listOf(toFiniteDumbAction(scenarioId))
    )
}

private fun DumbAction.toFiniteDumbAction(scenarioId: Identifier): DumbAction =
    when (this) {
        is DumbClick -> copy(
            scenarioId = scenarioId,
            isRepeatInfinite = false,
        )
        is DumbSwipe -> copy(
            scenarioId = scenarioId,
            isRepeatInfinite = false,
        )
        is DumbPause -> copy(
            scenarioId = scenarioId
        )
    }