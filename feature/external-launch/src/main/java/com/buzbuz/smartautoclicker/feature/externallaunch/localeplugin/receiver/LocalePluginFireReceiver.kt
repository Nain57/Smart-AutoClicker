/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.receiver

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.feature.externallaunch.R
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginActionExecutor
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginConfigurationCodec
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginContract
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginDeviceState
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginDirectLaunchTracker
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginLaunchFailureStore
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.ResolvedLocalePluginAction
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.notification.LocalePluginNotificationController
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.ui.LocalePluginExecutionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class LocalePluginFireReceiver : BroadcastReceiver() {

    @Inject internal lateinit var codec: LocalePluginConfigurationCodec
    @Inject internal lateinit var executor: LocalePluginActionExecutor
    @Inject internal lateinit var notifications: LocalePluginNotificationController
    @Inject internal lateinit var deviceState: LocalePluginDeviceState
    @Inject internal lateinit var directLaunchTracker: LocalePluginDirectLaunchTracker
    @Inject internal lateinit var launchFailureStore: LocalePluginLaunchFailureStore
    @Inject @Dispatcher(IO) internal lateinit var ioDispatcher: CoroutineDispatcher

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LocalePluginContract.ACTION_FIRE_SETTING) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            try {
                handleFire(context, intent)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleFire(context: Context, intent: Intent) {
        val configurationJson = LocalePluginContract.readConfigurationJson(intent)
        val configuration = codec.decode(configurationJson)
        if (configuration == null || configurationJson == null) {
            notifications.showError(R.string.locale_plugin_error_invalid)
            return
        }

        val requestId = UUID.randomUUID().toString()
        directLaunchTracker.markPending(requestId)
        launchFailureStore.clearLaunchPending()
        launchFailureStore.clearFallbackPending()
        launchFailureStore.markLaunchPending(requestId)

        when (val action = executor.resolve(configuration)) {
            null -> {
                directLaunchTracker.abandon(requestId)
                notifications.showError(R.string.locale_plugin_error_missing_scenario)
            }
            ResolvedLocalePluginAction.Stop -> {
                directLaunchTracker.abandon(requestId)
                notifications.cancelLaunchFallback()
                executor.executeStop()
            }
            is ResolvedLocalePluginAction.LaunchDumb -> {
                if (deferForOpenScenarioConfiguration(configurationJson, action.scenario.name, requestId)) {
                    return
                } else if (executor.areBasePermissionsReady(context)) {
                    directLaunchTracker.abandon(requestId)
                    if (directLaunchTracker.isLatest(requestId)) notifications.cancelLaunchFallback()
                    executor.launchDumb(action)
                } else requestUserCompletion(context, configurationJson, action.scenario.name, requestId)
            }
            is ResolvedLocalePluginAction.LaunchSmart -> {
                if (deferForOpenScenarioConfiguration(configurationJson, action.scenario.name, requestId)) {
                    return
                }
                if (executor.launchSmartWithCurrentProjection(action)) {
                    directLaunchTracker.abandon(requestId)
                    if (directLaunchTracker.isLatest(requestId)) notifications.cancelLaunchFallback()
                    return
                }
                requestUserCompletion(context, configurationJson, action.scenario.name, requestId)
            }
        }
    }

    /**
     * Do not replace a scenario while the user is editing it. The notification is an explicit,
     * user-controlled way to apply the requested launch after they finish their work.
     */
    private suspend fun deferForOpenScenarioConfiguration(
        configurationJson: String,
        scenarioName: String,
        requestId: String,
    ): Boolean {
        if (!executor.isScenarioConfigurationOpen()) return false

        Log.i(TAG, "Using notification fallback because the user is editing the current scenario")
        directLaunchTracker.abandon(requestId)
        launchFailureStore.markFallbackPending(requestId)
        notifications.showLaunchFallback(configurationJson, scenarioName, requestId)
        return true
    }

    private suspend fun requestUserCompletion(
        context: Context,
        configurationJson: String,
        scenarioName: String,
        requestId: String,
    ) {
        if (directLaunchTracker.hasAnotherInFlightRequest(requestId)) {
            Log.i(TAG, "Using notification fallback because another Locale launch is awaiting completion")
            directLaunchTracker.abandon(requestId)
            launchFailureStore.markFallbackPending(requestId)
            notifications.showLaunchFallback(configurationJson, scenarioName, requestId)
            return
        }

        if (!deviceState.canAttemptDirectLaunch()) {
            Log.i(TAG, "Using notification fallback because the device is locked or not interactive")
            directLaunchTracker.abandon(requestId)
            launchFailureStore.markFallbackPending(requestId)
            notifications.showLaunchFallback(configurationJson, scenarioName, requestId)
            return
        }

        val activityStarted = tryStartExecutionActivity(context, configurationJson, requestId)

        if (!activityStarted) {
            Log.w(TAG, "Direct Locale launch could not be sent; using notification fallback")
            directLaunchTracker.abandon(requestId)
            showDirectLaunchFallback(configurationJson, scenarioName, requestId)
            return
        }

        delay(DIRECT_LAUNCH_FALLBACK_DELAY_MS)
        if (!directLaunchTracker.consumeOpened(requestId) && directLaunchTracker.isLatest(requestId)) {
            Log.w(TAG, "Direct Locale launch did not open in time; using notification fallback")
            showDirectLaunchFallback(configurationJson, scenarioName, requestId)
        }
    }

    private suspend fun showDirectLaunchFallback(
        configurationJson: String,
        scenarioName: String,
        requestId: String,
    ) {
        if (!directLaunchTracker.isLatest(requestId)) return
        Log.i(TAG, "Recording failed direct launch and showing notification fallback")
        launchFailureStore.clearLaunchPending()
        launchFailureStore.markDirectLaunchFailed(requestId)
        launchFailureStore.markFallbackPending(requestId)
        // No Activity acknowledgement will arrive for this attempt in the normal fallback case.
        // Remove it from the tracker now so repeated blocked launches cannot grow the pending set.
        // A late Activity can still clear the persisted warning using its request id.
        directLaunchTracker.abandon(requestId)
        notifications.showLaunchFallback(configurationJson, scenarioName, requestId)
    }

    private fun tryStartExecutionActivity(
        context: Context,
        configurationJson: String,
        requestId: String,
    ): Boolean =
        runCatching {
            PendingIntent.getActivity(
                context,
                requestId.hashCode(),
                LocalePluginExecutionActivity.createIntent(context, configurationJson, requestId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ).send(
                context,
                0,
                null,
                null,
                null,
                null,
                backgroundActivityStartOptions(),
            )
        }.onFailure { throwable ->
            Log.w(TAG, "Can't directly open Locale plugin execution activity", throwable)
        }.isSuccess

    private fun backgroundActivityStartOptions(): Bundle? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
                )
            }.toBundle()
        }

        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityLaunchAllowed(true)
            }.toBundle()
        }

        return null
    }
}

private const val DIRECT_LAUNCH_FALLBACK_DELAY_MS = 1500L
private const val TAG = "LocalePluginFireReceiver"
