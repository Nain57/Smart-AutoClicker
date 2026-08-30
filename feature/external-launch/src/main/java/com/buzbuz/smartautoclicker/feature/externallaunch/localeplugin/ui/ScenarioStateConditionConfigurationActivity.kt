/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.ui

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.buzbuz.smartautoclicker.feature.externallaunch.R
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginContract
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.scenariostate.ScenarioStatePluginContract

class ScenarioStateConditionConfigurationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != LocalePluginContract.ACTION_EDIT_CONDITION) {
            finish()
            return
        }

        setResult(
            Activity.RESULT_OK,
            LocalePluginContract.createConditionResult(
                configurationJson = ScenarioStatePluginContract.CONFIGURATION_JSON,
                blurb = getString(R.string.locale_plugin_scenario_state_blurb),
                relevantVariables = ScenarioStatePluginContract.relevantVariables,
            ),
        )
        finish()
    }
}
