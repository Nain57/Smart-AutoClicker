/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import com.buzbuz.smartautoclicker.core.common.actions.external.ExternalActionEventContract
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginContract
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.externalaction.ExternalActionEventConfigurationCodec
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.scenariostate.ScenarioStatePluginContract
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.scenariostate.ScenarioStateProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocalePluginConditionQueryReceiver : BroadcastReceiver() {

    @Inject internal lateinit var codec: ExternalActionEventConfigurationCodec
    @Inject internal lateinit var stateProvider: ScenarioStateProvider

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ExternalActionEventContract.ACTION_QUERY_CONDITION) {
            resultCode = ExternalActionEventContract.RESULT_CONDITION_UNKNOWN
            return
        }

        if (LocalePluginContract.readConfigurationJson(intent) == ScenarioStatePluginContract.CONFIGURATION_JSON) {
            queryScenarioState(intent)
            return
        }

        val configuredName = codec
            .decode(ExternalActionEventContract.readConfigurationJson(intent))
            ?.externalActionName
        val firedName = ExternalActionEventContract.readFiredExternalActionName(intent)

        resultCode = when {
            configuredName == null || firedName == null ->
                ExternalActionEventContract.RESULT_CONDITION_UNKNOWN
            configuredName == firedName ->
                ExternalActionEventContract.RESULT_CONDITION_SATISFIED
            else ->
                ExternalActionEventContract.RESULT_CONDITION_UNSATISFIED
        }
    }

    private fun queryScenarioState(intent: Intent) {
        val orderedBroadcast = isOrderedBroadcast
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val snapshot = runCatching { stateProvider.getSnapshot() }
                .onFailure { Log.w(TAG, "Unable to read scenario state", it) }
                .getOrNull()
            val conditionResult = when {
                snapshot == null -> ExternalActionEventContract.RESULT_CONDITION_UNKNOWN
                else -> ExternalActionEventContract.RESULT_CONDITION_SATISFIED
            }
            val resultExtras = snapshot?.let {
                Bundle().apply {
                    putBundle(
                        LocalePluginContract.EXTRA_VARIABLES_BUNDLE,
                        ScenarioStatePluginContract.createVariables(it),
                    )
                }
            }

            val resultReceiver = getResultReceiver(intent)
            Log.d(
                TAG,
                "Scenario state query: ordered=$orderedBroadcast, resultReceiver=${resultReceiver != null}, " +
                    "open=${snapshot?.isScenarioOpen}, state=${snapshot?.state?.value}",
            )
            resultReceiver?.send(conditionResult, resultExtras)
            pendingResult.setResultCode(conditionResult)
            pendingResult.setResultExtras(resultExtras)
            pendingResult.finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun getResultReceiver(intent: Intent): ResultReceiver? =
        runCatching {
            intent.getParcelableExtra<ResultReceiver>(LocalePluginContract.EXTRA_CONDITION_RESULT_RECEIVER)
        }.onFailure { Log.w(TAG, "Unable to read the host result receiver", it) }
            .getOrNull()
}

private const val TAG = "ScenarioStatePlugin"
