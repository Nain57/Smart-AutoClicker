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
package com.buzbuz.smartautoclicker.feature.permissions.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.feature.permissions.PermissionsController
import com.buzbuz.smartautoclicker.feature.permissions.R
import com.buzbuz.smartautoclicker.feature.permissions.model.Permission
import com.buzbuz.smartautoclicker.feature.permissions.model.PermissionAccessibilityService
import com.buzbuz.smartautoclicker.feature.permissions.model.PermissionOverlay
import com.buzbuz.smartautoclicker.feature.permissions.model.PermissionPostNotification

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class PermissionDialogViewModel @Inject constructor(
    private val permissionsController: PermissionsController,
) : ViewModel() {

    val dialogUiState: StateFlow<PermissionDialogUiState?> = permissionsController.currentRequestedPermission
        .map { it?.toPermissionDialogUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun isPermissionGranted(context: Context): Boolean =
        permissionsController.currentRequestedPermission.value?.checkIfGranted(context) ?: false

    fun initResultLauncherIfNeeded(fragment: Fragment, onResult: (granted: Boolean, optional: Boolean) -> Unit) {
        val permission = permissionsController.currentRequestedPermission.value ?: return

        if (permission is Permission.Dangerous) {
            permission.initResultLauncher(fragment) { isGranted ->
                onResult(isGranted, permission.isOptional())
            }
        }
    }

    fun startPermissionFlow(context: Context) {
        val permission = permissionsController.currentRequestedPermission.value ?: return
        permission.startRequestFlowIfNeeded(context)
    }

    fun shouldBeDismissedOnResume(context: Context): Boolean {
        val permission = permissionsController.currentRequestedPermission.value ?: return true
        return permission is Permission.Special && (permission.checkIfGranted(context) || permission.isOptional())
    }
}

internal data class PermissionDialogUiState(
    val permission: Permission,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
)

private fun Permission.toPermissionDialogUiState(): PermissionDialogUiState =
    when (this) {
        PermissionOverlay -> PermissionDialogUiState(
            permission = this,
            titleRes = R.string.dialog_title_permission_overlay,
            descriptionRes = R.string.message_permission_desc_overlay,
        )

        PermissionPostNotification -> PermissionDialogUiState(
            permission = this,
            titleRes = R.string.dialog_title_permission_notification,
            descriptionRes = R.string.message_permission_desc_notification,
        )

        is PermissionAccessibilityService -> PermissionDialogUiState(
            permission = this,
            titleRes = R.string.dialog_title_permission_accessibility,
            descriptionRes = R.string.message_permission_desc_accessibility,
        )
    }