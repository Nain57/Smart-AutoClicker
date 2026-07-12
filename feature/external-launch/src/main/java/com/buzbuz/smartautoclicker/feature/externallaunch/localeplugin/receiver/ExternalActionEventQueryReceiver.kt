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
import com.buzbuz.smartautoclicker.core.common.actions.external.ExternalActionEventContract
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.externalaction.ExternalActionEventConfigurationCodec
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ExternalActionEventQueryReceiver : BroadcastReceiver() {

    @Inject internal lateinit var codec: ExternalActionEventConfigurationCodec

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ExternalActionEventContract.ACTION_QUERY_CONDITION) {
            resultCode = ExternalActionEventContract.RESULT_CONDITION_UNKNOWN
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
}
