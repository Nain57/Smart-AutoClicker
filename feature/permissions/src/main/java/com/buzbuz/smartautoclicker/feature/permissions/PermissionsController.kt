/*
 * Copyright (C) 2024 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.feature.permissions

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

import com.buzbuz.smartautoclicker.feature.permissions.model.Permission
import com.buzbuz.smartautoclicker.feature.permissions.ui.PermissionDialogFragment

import com.google.android.material.dialog.MaterialAlertDialogBuilder

import dagger.hilt.android.scopes.ActivityRetainedScoped

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ActivityRetainedScoped
class PermissionsController @Inject constructor() {

    private val permissionsRequestedLeft: MutableSet<Permission> =
        mutableSetOf()

    private val _currentRequestedPermission: MutableStateFlow<Permission?> =
        MutableStateFlow(null)
    val currentRequestedPermission: StateFlow<Permission?> = _currentRequestedPermission

    private var allGrantedCallback: (() -> Unit)? = null


    fun startPermissionsUiFlow(activity: AppCompatActivity, permissions: List<Permission>, onAllGranted: () -> Unit) {
        if (permissions.isEmpty()) return

        permissionsRequestedLeft.clear()
        allGrantedCallback = onAllGranted

        permissions.forEach { permission ->
            if (!permission.checkIfGranted(activity)) permissionsRequestedLeft.add(permission)
        }

        Log.i(TAG, "Requesting missing permissions $permissions")

        handleNextPermission(activity)
    }

    private fun handleNextPermission(activity: AppCompatActivity) {
        // All granted ? We are good
        if (permissionsRequestedLeft.isEmpty()) {
            Log.i(TAG, "All permission are granted !")
            notifyAllGranted()
            return
        }

        // Take the next permission on the list
        val nextPermission = permissionsRequestedLeft.popFirst()

        // This permission is optional and has already been requested, skip it
        if (nextPermission.isOptionalAndRequestedBefore(activity)) {
            Log.d(TAG, "Skipping already requested permission $nextPermission")
            handleNextPermission(activity)
            return
        }

        // Show the dialog and handle the result
        Log.i(TAG, "show permission dialog for $nextPermission")
        activity.showPermissionDialogFragment(nextPermission) { isGranted ->
            Log.i(TAG, "onPermissionDialogResult: $nextPermission isGranted=$isGranted")

            if (isGranted || nextPermission.isOptional()) handleNextPermission(activity)
            else activity.showMandatoryPermissionDeniedDialog()
        }
    }

    private fun notifyAllGranted() {
        allGrantedCallback?.invoke()
        clear()
    }

    private fun clear() {
        allGrantedCallback = null
        _currentRequestedPermission.value = null
        permissionsRequestedLeft.clear()
    }

    private fun AppCompatActivity.showPermissionDialogFragment(permission: Permission, resultListener: (isGranted: Boolean) -> Unit) {
        // Setup the result listener on the permission request
        supportFragmentManager.setFragmentResultListener(FRAGMENT_RESULT_KEY_PERMISSION_STATE, this) { _, bundle ->
            _currentRequestedPermission.value = null
            resultListener(bundle.getBoolean(EXTRA_RESULT_KEY_PERMISSION_STATE))
        }

        // Show the permission request dialog
        _currentRequestedPermission.value = permission
        PermissionDialogFragment().show(supportFragmentManager, FRAGMENT_TAG_PERMISSION_DIALOG)
    }

    private fun AppCompatActivity.showMandatoryPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_title_permission_mandatory_denied)
            .setMessage(R.string.message_permission_mandatory_denied)
            .setPositiveButton(android.R.string.ok) { _, _ -> clear() }
            .setOnCancelListener { clear() }
            .create()
            .show()
    }

    private fun Permission.isOptionalAndRequestedBefore(context: Context) =
        isOptional() && hasBeenRequestedBefore(context)

    private fun MutableSet<Permission>.popFirst() : Permission =
        first().also(::remove)
}

/** Tag for permission dialog fragment. */
internal const val FRAGMENT_TAG_PERMISSION_DIALOG = "PermissionDialog"

/** Fragment result key for the permission granted or not state once dialog is closed. */
internal const val FRAGMENT_RESULT_KEY_PERMISSION_STATE = ":$FRAGMENT_TAG_PERMISSION_DIALOG:state"
/**
 * Key for [FRAGMENT_RESULT_KEY_PERMISSION_STATE] result bundle.
 * Boolean indicating the permission state.
 */
internal const val EXTRA_RESULT_KEY_PERMISSION_STATE = "$FRAGMENT_RESULT_KEY_PERMISSION_STATE:isGranted"


private const val TAG = "PermissionsController"