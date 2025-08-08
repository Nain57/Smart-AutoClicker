
package com.buzbuz.smartautoclicker.localservice

import android.content.Intent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

interface ILocalService {
    fun startDumbScenario(dumbScenario: DumbScenario)
    fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
    fun stop()
    fun release()
}