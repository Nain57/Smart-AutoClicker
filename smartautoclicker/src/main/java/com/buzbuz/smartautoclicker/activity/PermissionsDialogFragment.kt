/*
 * Copyright (C) 2022 Nain57
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
package com.buzbuz.smartautoclicker.activity

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.baseui.dialog.setCustomTitle
import com.buzbuz.smartautoclicker.SmartAutoClickerService

/**
 * Displays the state of the permission and provide a way to access their respective settings.
 * Activity attaching this fragment must implements [PermissionDialogListener] to be notified upon grant success to all
 * permissions.
 */
class PermissionsDialogFragment : DialogFragment() {

    companion object {

        /** Tag for permission dialog fragment. */
        const val FRAGMENT_TAG_PERMISSION_DIALOG = "PermissionDialog"
        /** Intent extra bundle key for the Android settings app. */
        private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
        /** Intent extra bundle key for the Android settings app. */
        private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

        /**
         * Creates a new instance of this fragment.
         * @return the new fragment.
         */
        fun newInstance() : PermissionsDialogFragment {
            return PermissionsDialogFragment()
        }
    }

    /**
     * Listener to be implementation by the activity attaching this fragment receiving the all permission granted
     * information.
     */
    interface PermissionDialogListener {
        /** Called when all permissions are granted and the user press ok. */
        fun onPermissionsGranted()
    }

    /** ViewModel providing the click scenarios data to the UI. */
    private val scenarioViewModel: ScenarioViewModel by activityViewModels()
    /** View for the overlay permission. */
    private lateinit var overlayView: View
    /** View for the state of the overlay permission. */
    private lateinit var overlayStateView: ImageView
    /** View for the accessibility service permission. */
    private lateinit var accessibilityView: View
    /** View for the state of the accessibility service permission. */
    private lateinit var accessibilityStateView: ImageView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setCustomTitle(R.layout.view_dialog_title, R.string.dialog_permissions_title)
            .setView(R.layout.dialog_permissions)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                (activity?.supportFragmentManager?.findFragmentByTag(FRAGMENT_TAG_SCENARIO_LIST) as PermissionDialogListener)
                    .onPermissionsGranted()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            overlayStateView = it.findViewById(R.id.img_config_overlay_status)
            overlayView = it.findViewById(R.id.item_overlay_permission)
            overlayView.setOnClickListener{ onOverlayClicked() }
            accessibilityStateView = it.findViewById(R.id.img_config_accessibility_status)
            accessibilityView = it.findViewById(R.id.item_accessibility_permission)
            accessibilityView.setOnClickListener { onAccessibilityClicked() }
        }
    }

    override fun onResume() {
        super.onResume()
        setConfigStateDrawable(overlayStateView, scenarioViewModel.isOverlayPermissionValid())
        setConfigStateDrawable(accessibilityStateView, scenarioViewModel.isAccessibilityPermissionValid())
        (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
            scenarioViewModel.isOverlayPermissionValid() && scenarioViewModel.isAccessibilityPermissionValid()
    }

    /**
     * Called when the user clicks on the overlay permission state.
     * This will start the Android Settings Activity for the overlay permission of this application.
     */
    private fun onOverlayClicked() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${requireContext().packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)

        requireContext().startActivity(intent)
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
        val showArgs = requireContext().packageName + "/" + SmartAutoClickerService::class.java.name
        bundle.putString(EXTRA_FRAGMENT_ARG_KEY, showArgs)
        intent.putExtra(EXTRA_FRAGMENT_ARG_KEY, showArgs)
        intent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)

        requireContext().startActivity(intent)
    }

    /**
     * Update the provided permission state view according to the state parameter.
     *
     * @param view the TextView displaying the permission state.
     * @param state the state of the permission.
     */
    private fun setConfigStateDrawable(view: ImageView, state: Boolean) {
        if (state) {
            view.setImageResource(R.drawable.ic_confirm)
            view.drawable.setTint(Color.GREEN)
        } else {
            view.setImageResource(R.drawable.ic_cancel)
            view.drawable.setTint(Color.RED)
        }
    }
}