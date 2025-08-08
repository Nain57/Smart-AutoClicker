
package com.buzbuz.smartautoclicker.core.domain.model.scenario

import com.buzbuz.smartautoclicker.core.base.ScenarioStats
import com.buzbuz.smartautoclicker.core.database.entity.ScenarioEntity
import com.buzbuz.smartautoclicker.core.domain.utils.asIdentifier

internal object ScenarioTestsData {

    /* ------- Scenario Data ------- */

    const val SCENARIO_ID = 42L
    const val SCENARIO_NAME = "ClickScenario"
    const val SCENARIO_DETECTION_QUALITY = 500
    const val SCENARIO_RANDOMIZE = false

    fun getNewScenarioEntity(
        id: Long = SCENARIO_ID,
        name: String = SCENARIO_NAME,
        detectionQuality: Int = SCENARIO_DETECTION_QUALITY,
        randomize: Boolean = SCENARIO_RANDOMIZE,
    ) = ScenarioEntity(id, name, detectionQuality, randomize)

    fun getNewScenario(
        id: Long = SCENARIO_ID,
        name: String = SCENARIO_NAME,
        detectionQuality: Int = SCENARIO_DETECTION_QUALITY,
        randomize: Boolean = SCENARIO_RANDOMIZE,
        eventCount: Int = 0,
        stats: ScenarioStats? = null,
    ) = Scenario(id.asIdentifier(), name, detectionQuality, randomize, eventCount, stats)

    fun defaultStats(): ScenarioStats =
        ScenarioStats(lastStartTimestampMs=0, startCount=0)
}