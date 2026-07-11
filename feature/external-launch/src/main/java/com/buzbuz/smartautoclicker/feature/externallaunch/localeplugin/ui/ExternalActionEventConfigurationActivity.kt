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
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.buzbuz.smartautoclicker.core.common.actions.external.ExternalActionEventContract
import com.buzbuz.smartautoclicker.feature.externallaunch.R
import com.buzbuz.smartautoclicker.feature.externallaunch.databinding.ActivityExternalActionEventConfigurationBinding
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.externalaction.ExternalActionEventConfigurationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExternalActionEventConfigurationActivity : AppCompatActivity() {

    private val viewModel: ExternalActionEventConfigurationViewModel by viewModels()
    private lateinit var binding: ActivityExternalActionEventConfigurationBinding
    private lateinit var namesAdapter: ArrayAdapter<String>
    private var restoredName: String? = null
    private var selectedName: String? = null
    private var hasAppliedRestore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action != ExternalActionEventContract.ACTION_EDIT_EVENT) {
            finish()
            return
        }

        binding = ActivityExternalActionEventConfigurationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        namesAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf())
        binding.externalActionName.setAdapter(namesAdapter)
        restoredName = viewModel
            .decodeConfiguration(ExternalActionEventContract.readConfigurationJson(intent))
            ?.externalActionName

        binding.externalActionName.setOnItemClickListener { _, _, position, _ ->
            selectedName = namesAdapter.getItem(position)
            render()
        }
        binding.cancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
        binding.save.setOnClickListener { saveConfiguration() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.knownExternalActionNames.collect(::updateNames)
            }
        }
    }

    private fun updateNames(knownNames: List<String>) {
        if (!hasAppliedRestore) {
            hasAppliedRestore = true
            selectedName = restoredName ?: knownNames.firstOrNull()
        } else if (selectedName == null) {
            selectedName = knownNames.firstOrNull()
        } else if (selectedName != restoredName && selectedName !in knownNames) {
            selectedName = knownNames.firstOrNull()
        }

        val namesForDisplay = buildList {
            if (restoredName != null && restoredName !in knownNames) add(restoredName!!)
            addAll(knownNames)
        }

        namesAdapter.clear()
        namesAdapter.addAll(namesForDisplay)
        binding.externalActionName.setText(selectedName.orEmpty(), false)
        render()
    }

    private fun render() {
        val name = selectedName
        val isMissingRestoredName = name != null && name == restoredName && namesAdapter.getPosition(name) == 0 &&
            viewModel.knownExternalActionNames.value.none { it == name }

        binding.save.isEnabled = !name.isNullOrBlank()
        binding.message.text = when {
            namesAdapter.isEmpty -> getString(R.string.external_action_event_empty)
            isMissingRestoredName -> getString(R.string.external_action_event_missing_name)
            else -> getString(R.string.external_action_event_description)
        }
        binding.message.visibility = View.VISIBLE
    }

    private fun saveConfiguration() {
        val name = selectedName?.trim()?.takeIf { it.isNotEmpty() } ?: return
        setResult(
            Activity.RESULT_OK,
            ExternalActionEventContract.createConfigurationResult(
                configurationJson = viewModel.encodeConfiguration(name),
                blurb = getString(R.string.external_action_event_blurb, name),
            ),
        )
        finish()
    }
}
