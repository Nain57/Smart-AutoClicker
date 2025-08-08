
package com.buzbuz.smartautoclicker.localservice

import android.content.Intent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

interface ILocalService {
    fun startSmartScenario(resultCode: Int, data: Intent, scenario: Scenario)
    fun stop()
    fun release()
}