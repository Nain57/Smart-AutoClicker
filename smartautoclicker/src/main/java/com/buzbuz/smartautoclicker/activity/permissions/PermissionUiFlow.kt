/*
 * Copyright (C) 2023 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.activity.permissions

import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager

fun AppCompatActivity.startPermissionFlow(
    fragmentManager: FragmentManager,
    onAllGranted: () -> Unit,
    onMandatoryDenied: () -> Unit,
) {
    val permissionsLeft = buildSet {
        Permission.Type.values().forEach { permissionType ->
            if (!permissionType.permission.isGranted(this@startPermissionFlow)) add(permissionType)
        }
    }.toMutableSet()

    showNextPermissionDialog(fragmentManager, permissionsLeft, onAllGranted, onMandatoryDenied)
}

private fun AppCompatActivity.showNextPermissionDialog(
    fragmentManager: FragmentManager,
    permissionsLeft: MutableSet<Permission.Type>,
    onAllGranted: () -> Unit,
    onMandatoryDenied: () -> Unit,
) {
    // All granted ? We are good
    if (permissionsLeft.isEmpty()) {
        Log.d(TAG, "All permission are granted !")
        onAllGranted()
        return
    }

    // Check the next permission on the list
    val nextPermissionType = permissionsLeft.first()
    val nextPermission = nextPermissionType.permission

    // This permission is optional and has already been requested, skip it
    if (nextPermission is Permission.Dangerous && nextPermission.isOptional() &&
            nextPermission.hasBeenRequestedBefore(this)) {

        Log.d(TAG, "Skipping already requested permission ${nextPermissionType.name}")
        permissionsLeft.remove(nextPermissionType)
        showNextPermissionDialog(fragmentManager, permissionsLeft, onAllGranted, onMandatoryDenied)
        return
    }

    // Setup the result listener on the permission request
    setPermissionDialogResultListener(fragmentManager) { isGranted ->
        Log.d(TAG, "onPermissionDialogResult: ${nextPermissionType.name} isGranted=$isGranted")

        if (isGranted || nextPermissionType.permission.isOptional()) {
            permissionsLeft.remove(nextPermissionType)
            showNextPermissionDialog(fragmentManager, permissionsLeft, onAllGranted, onMandatoryDenied)
        } else {
            onMandatoryDenied()
        }
    }

    // Show the permission request dialog
    Log.d(TAG, "showNextPermissionDialog: ${nextPermissionType.name}")
    PermissionDialogFragment.newInstance(nextPermissionType).show(
        fragmentManager,
        PermissionDialogFragment.FRAGMENT_TAG_PERMISSION_DIALOG,
    )
}

private fun AppCompatActivity.setPermissionDialogResultListener(
    fragmentManager: FragmentManager,
    resultListener: (isGranted: Boolean) -> Unit
) {
    fragmentManager.setFragmentResultListener(
        PermissionDialogFragment.FRAGMENT_RESULT_KEY_PERMISSION_STATE,
        this)
    { _, bundle ->
        resultListener(bundle.getBoolean(PermissionDialogFragment.EXTRA_RESULT_KEY_PERMISSION_STATE))
    }
}

private const val TAG = "PermissionUiFlow"