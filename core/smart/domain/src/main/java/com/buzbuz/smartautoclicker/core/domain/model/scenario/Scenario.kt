
package com.buzbuz.smartautoclicker.core.domain.model.scenario

import com.buzbuz.smartautoclicker.core.base.ScenarioStats
import com.buzbuz.smartautoclicker.core.base.interfaces.Identifiable
import com.buzbuz.smartautoclicker.core.base.identifier.Identifier

/**
 * Scenario of events.
 *
 * @param id the unique identifier for the scenario.
 * @param name the name of the scenario.
 * @param detectionQuality the quality of the detection algorithm. Lower value means faster detection but poorer
 *                         quality, while higher values means better and slower detection.
 * @param randomize tells if the actions values should be randomized a bit.
 * @param eventCount the number of events in this scenario. Default value is 0.
 */
data class Scenario(
    override val id: Identifier,
    val name: String,
    val detectionQuality: Int,
    val randomize: Boolean = false,
    val eventCount: Int = 0,
    val stats: ScenarioStats? = null,
): Identifiable
