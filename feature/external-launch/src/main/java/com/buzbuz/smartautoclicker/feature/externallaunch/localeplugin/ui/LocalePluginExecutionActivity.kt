/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.buzbuz.smartautoclicker.core.display.recorder.MediaProjectionRequest
import com.buzbuz.smartautoclicker.feature.externallaunch.R
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginDirectLaunchTracker
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginLaunchFailureStore
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.ResolvedLocalePluginAction
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.notification.LocalePluginNotificationController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LocalePluginExecutionActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_CONFIGURATION_JSON =
            "com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.CONFIGURATION_JSON"
        private const val EXTRA_REQUEST_ID =
            "com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.REQUEST_ID"
        private const val EXTRA_FROM_FALLBACK =
            "com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.FROM_FALLBACK"

        fun createIntent(
            context: Context,
            configurationJson: String,
            requestId: String? = null,
        ): Intent =
            Intent(context, LocalePluginExecutionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_CONFIGURATION_JSON, configurationJson)
                .apply {
                    requestId?.let { putExtra(EXTRA_REQUEST_ID, it) }
                }

        fun createFallbackIntent(
            context: Context,
            configurationJson: String,
            requestId: String,
        ): Intent =
            createIntent(context, configurationJson, requestId)
                .putExtra(EXTRA_FROM_FALLBACK, true)
    }

    @Inject internal lateinit var notifications: LocalePluginNotificationController
    @Inject internal lateinit var directLaunchTracker: LocalePluginDirectLaunchTracker
    @Inject internal lateinit var launchFailureStore: LocalePluginLaunchFailureStore
    private val viewModel: LocalePluginExecutionViewModel by viewModels()
    private val mediaProjectionRequest = MediaProjectionRequest()
    private var smartAction: ResolvedLocalePluginAction.LaunchSmart? = null
    private var configurationJson: String? = null
    private var requestId: String? = null
    private var launchedFromFallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locale_plugin_execution)
        mediaProjectionRequest.registerForActivityResult(this)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(launchIntent: Intent) {
        configurationJson = launchIntent.getStringExtra(EXTRA_CONFIGURATION_JSON)
        requestId = launchIntent.getStringExtra(EXTRA_REQUEST_ID)
        launchedFromFallback = launchIntent.getBooleanExtra(EXTRA_FROM_FALLBACK, false)

        lifecycleScope.launch {
            val accepted = requestId?.let { id ->
                if (!launchedFromFallback) {
                    directLaunchTracker.claimDirectLaunch(id) ||
                        launchFailureStore.consumeLaunchPending(id).also { recovered ->
                            if (recovered) directLaunchTracker.claimRecoveredLaunch(id)
                        }
                } else {
                    directLaunchTracker.claimFallbackLaunch(id) ||
                        launchFailureStore.consumeFallbackPending(id).also { recovered ->
                            if (recovered) directLaunchTracker.claimRecoveredLaunch(id)
                        } || launchFailureStore.consumeLaunchPending(id).also { recovered ->
                            if (recovered) directLaunchTracker.claimRecoveredLaunch(id)
                        }
                }
            } ?: true
            if (!accepted) {
                finishAndRemoveTask()
                return@launch
            }

            requestId?.let { id ->
                notifications.cancelLaunchFallback()
                launchFailureStore.consumeLaunchPending(id)
                launchFailureStore.consumeFallbackPending(id)
                if (!launchedFromFallback) launchFailureStore.clearDirectLaunchFailure(id)
            }
            viewModel.resolve(configurationJson, ::handleResolvedAction)
        }
    }

    private fun handleResolvedAction(action: ResolvedLocalePluginAction?) {
        if (!isCurrentRequest()) {
            close()
            return
        }
        when (action) {
            null -> fail(R.string.locale_plugin_error_invalid)
            ResolvedLocalePluginAction.Stop -> {
                viewModel.executeStop()
                close()
            }
            is ResolvedLocalePluginAction.LaunchDumb -> requestPermissions {
                if (isCurrentRequest()) {
                    viewModel.launchDumb(action)
                    close()
                }
            }
            is ResolvedLocalePluginAction.LaunchSmart -> {
                smartAction = action
                requestPermissions {
                    mediaProjectionRequest.showMediaProjectionWarning(
                        context = this,
                        forceEntireScreen = viewModel.isEntireScreenCaptureForced(),
                        onSuccess = success@{ resultCode, data ->
                            if (!isCurrentRequest()) {
                                close()
                                return@success
                            }
                            viewModel.launchSmart(resultCode, data, action)
                            close()
                        },
                        onFailure = {
                            if (isCurrentRequest()) fail(R.string.locale_plugin_error_projection)
                            else close()
                        },
                        onError = ::handleProjectionLaunchError,
                    )
                }
            }
        }
    }

    private fun requestPermissions(onGranted: () -> Unit) {
        viewModel.requestPermissions(
            activity = this,
            onAllGranted = { if (isCurrentRequest()) onGranted() else close() },
            onMandatoryDenied = {
                if (isCurrentRequest()) fail(R.string.locale_plugin_error_permissions)
                else close()
            },
        )
    }

    private fun fail(messageRes: Int) {
        notifications.showError(messageRes)
        close()
    }

    private fun handleProjectionLaunchError() {
        val id = requestId
        val configuration = configurationJson
        val action = smartAction
        if (!isCurrentRequest()) {
            close()
            return
        }
        if (launchedFromFallback || id == null || configuration == null || action == null) {
            fail(R.string.locale_plugin_error_projection)
            return
        }

        lifecycleScope.launch {
            launchFailureStore.markDirectLaunchFailed(id)
            launchFailureStore.markFallbackPending(id)
            notifications.showLaunchFallback(configuration, action.scenario.name, id)
            close()
        }
    }

    override fun onDestroy() {
        // A back press or system finish does not necessarily reach close(). Do not leave the
        // process-wide launch serializer stuck after this translucent helper disappears.
        if (!isChangingConfigurations) directLaunchTracker.markExecutionClosed(requestId)
        super.onDestroy()
    }

    private fun close() {
        directLaunchTracker.markExecutionClosed(requestId)
        finishAndRemoveTask()
    }

    private fun isCurrentRequest(): Boolean =
        requestId == null || directLaunchTracker.isCurrentExecution(requestId)
}
