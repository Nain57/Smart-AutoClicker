
package com.buzbuz.smartautoclicker.core.common.permissions.ui

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.common.permissions.PermissionsController
import com.buzbuz.smartautoclicker.core.common.permissions.R
import com.buzbuz.smartautoclicker.core.common.permissions.model.Permission
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionAccessibilityService
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionOverlay
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification

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
                onResult(isGranted, permission.isOptional)
            }
        }
    }

    fun startPermissionFlow(context: Context) {
        val permission = permissionsController.currentRequestedPermission.value ?: return
        permission.startRequestFlowIfNeeded(context)
    }

    fun shouldBeDismissedOnResume(context: Context): Boolean {
        val permission = permissionsController.currentRequestedPermission.value ?: return true
        return permission.checkIfGranted(context) || (permission.isOptional && permission.hasBeenRequestedBefore(context))
    }
}

internal data class PermissionDialogUiState(
    val permission: Permission,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
)

private fun Permission.toPermissionDialogUiState(): PermissionDialogUiState =
    when (this) {
        is PermissionOverlay -> PermissionDialogUiState(
            permission = this,
            titleRes = R.string.dialog_title_permission_overlay,
            descriptionRes = R.string.message_permission_desc_overlay,
        )

        is PermissionPostNotification -> PermissionDialogUiState(
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