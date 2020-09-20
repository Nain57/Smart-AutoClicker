/*
 * Copyright (C) 2020 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.extensions.setRightCompoundDrawable
import com.buzbuz.smartautoclicker.service.SmartAutoClickerService

/**
 * Handles the views displaying the state of the permissions required in order for this application to works.
 *
 * @param configView the root view containing all permissions state views.
 * @param localServiceSupplier supplier providing the state of the [SmartAutoClickerService].
 */
class ConfigViewController(
    private val configView: View,
    private val localServiceSupplier: () -> SmartAutoClickerService.LocalService?
) : LifecycleObserver {

    private companion object {
        /** Intent extra bundle key for the Android settings app. */
        private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
        /** Intent extra bundle key for the Android settings app. */
        private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
    }

    /** View for the state of the overlay permission. */
    private lateinit var overlayView: TextView
    /** View for the state of the accessibility service permission. */
    private lateinit var accessibilityView: TextView
    /** The Android context. */
    private val context: Context
        get() = overlayView.context

    /**
     * Tells if the permissions configuration is valid.
     *
     * @return true if all permissions are granted, false if not.
     */
    fun isConfigurationValid() : Boolean {
        return isOverlayConfigValid() && isAccessibilityConfigValid()
    }

    /** Inflate the permissions state views contained in the [configView] and setup their click listeners. */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun inflateViews() {
        overlayView = configView.findViewById(R.id.text_config_overlay)
        overlayView.setOnClickListener { onOverlayClicked() }
        accessibilityView = configView.findViewById(R.id.text_config_accessibility)
        accessibilityView.setOnClickListener { onAccessibilityClicked() }
    }

    /** Refresh the displayed permissions state. */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun refreshConfigurationState() {
        setConfigStateDrawable(overlayView, isOverlayConfigValid())
        setConfigStateDrawable(accessibilityView, isAccessibilityConfigValid())
    }

    /**
     * Called when the user clicks on the overlay permission state.
     * This will start the Android Settings Activity for the overlay permission of this application.
     */
    private fun onOverlayClicked() {
        val context = context
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)

        context.startActivity(intent)
    }

    /**
     * Called when the user clicks on the Accessibility Service permission state.
     * This will open the Android Settings Activity for the list of available Accessibility Service. Note that it seems
     * impossible to directly start this application accessibility service management screen, but only the list of all
     * Accessibility services.
     */
    private fun onAccessibilityClicked() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)

        val bundle = Bundle()
        val showArgs = context.packageName + "/" + SmartAutoClickerService::class.java.name
        bundle.putString(EXTRA_FRAGMENT_ARG_KEY, showArgs)
        intent.putExtra(EXTRA_FRAGMENT_ARG_KEY, showArgs)
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)

        context.startActivity(intent)
    }

    /**
     * Tells if the overlay permission is granted for this application.
     *
     * @return true if the permission is granted, false if not.
     */
    private fun isOverlayConfigValid(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Tells if the Accessibility Service of this application is started.
     *
     * @return true if the service is started, false if not.
     */
    private fun isAccessibilityConfigValid(): Boolean {
        return localServiceSupplier.invoke() != null
    }

    /**
     * Update the provided permission state view according to the state parameter.
     *
     * @param view the TextView displaying the permission state.
     * @param state the state of the permission.
     */
    private fun setConfigStateDrawable(view: TextView, state: Boolean) =
        if (state)
            view.setRightCompoundDrawable(R.drawable.ic_confirm, Color.GREEN)
        else
            view.setRightCompoundDrawable(R.drawable.ic_cancel, Color.RED)
}
