
package com.buzbuz.smartautoclicker.feature.qstile.domain

import android.content.Intent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

interface QSTileActionHandler {
    fun isRunning() : Boolean
    fun startDumbScenario(dumbScenario: DumbScenario)
    fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
    fun stop()
}