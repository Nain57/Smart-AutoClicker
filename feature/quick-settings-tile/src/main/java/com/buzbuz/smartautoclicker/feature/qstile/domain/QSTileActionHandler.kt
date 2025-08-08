
package com.buzbuz.smartautoclicker.feature.qstile.domain

import android.content.Intent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

interface QSTileActionHandler {
    fun isRunning() : Boolean
    fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
    fun stop()
}