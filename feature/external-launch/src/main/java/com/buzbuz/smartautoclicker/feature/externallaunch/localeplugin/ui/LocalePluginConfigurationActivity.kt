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
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.feature.externallaunch.R
import com.buzbuz.smartautoclicker.feature.externallaunch.databinding.ActivityLocalePluginConfigurationBinding
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginConfiguration
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginContract
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginOperation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LocalePluginConfigurationActivity : AppCompatActivity() {

    private val viewModel: LocalePluginConfigurationViewModel by viewModels()
    private lateinit var binding: ActivityLocalePluginConfigurationBinding
    private lateinit var scenarioAdapter: LocalePluginScenarioAdapter
    private var selectedScenario: LocalePluginScenarioItem? = null
    private var restoredConfiguration: LocalePluginConfiguration? = null
    private var hasAppliedRestore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != LocalePluginContract.ACTION_EDIT_SETTING) {
            finish()
            return
        }

        binding = ActivityLocalePluginConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scenarioAdapter = LocalePluginScenarioAdapter(this)
        binding.scenario.setAdapter(scenarioAdapter)
        restoredConfiguration = viewModel.decodeConfiguration(LocalePluginContract.readConfigurationJson(intent))

        binding.scenario.setOnItemClickListener { _, _, position, _ ->
            selectedScenario = scenarioAdapter.getItem(position)
            render()
        }
        binding.cancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        binding.save.setOnClickListener {
            viewModel.requestFallbackNotificationPermission(this, ::saveConfiguration)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scenarios.collect(::updateScenarios)
            }
        }
    }

    private fun updateScenarios(scenarios: List<LocalePluginScenarioItem>) {
        scenarioAdapter.replace(scenarios)
        if (!hasAppliedRestore) {
            hasAppliedRestore = true
            restoredConfiguration?.let { restored ->
                selectedScenario = scenarios.find {
                    it.id == restored.scenarioId && it.isSmart == restored.isSmart
                }
            }
            if (restoredConfiguration == null) selectedScenario = scenarios.firstOrNull()
        } else {
            selectedScenario = selectedScenario?.let { selected ->
                scenarios.find { it.id == selected.id && it.isSmart == selected.isSmart }
            }
        }
        binding.scenario.setText(selectedScenario?.name.orEmpty(), false)
        render()
    }

    private fun render() {
        binding.save.isEnabled = selectedScenario != null

        val message = when {
            scenarioAdapter.isEmpty -> getString(R.string.locale_plugin_no_scenarios)
            restoredConfiguration?.operation == LocalePluginOperation.LAUNCH &&
                restoredConfiguration?.scenarioId != null && selectedScenario == null ->
                getString(R.string.locale_plugin_deleted_scenario)
            selectedScenario?.isSmart == true -> getString(R.string.locale_plugin_smart_note)
            else -> null
        }
        binding.message.text = message
        binding.message.visibility = if (message == null) View.GONE else View.VISIBLE
    }

    private fun saveConfiguration() {
        if (selectedScenario != null) finishSavingConfiguration()
    }

    private fun finishSavingConfiguration() {
        val scenario = selectedScenario ?: return
        val configuration = LocalePluginConfiguration(
            operation = LocalePluginOperation.LAUNCH,
            scenarioId = scenario.id,
            isSmart = scenario.isSmart,
        )
        val blurb = getString(
            R.string.locale_plugin_blurb_launch,
            scenario.name,
            getString(
                if (scenario.isSmart) R.string.locale_plugin_type_smart else R.string.locale_plugin_type_dumb
            ),
        )
        setResult(
            Activity.RESULT_OK,
            LocalePluginContract.createResult(viewModel.encodeConfiguration(configuration), blurb),
        )
        finish()
    }
}
