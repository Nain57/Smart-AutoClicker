
package com.buzbuz.smartautoclicker.feature.qstile.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.core.common.permissions.PermissionsController
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionAccessibilityService
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionOverlay
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.core.settings.SettingsRepository
import com.buzbuz.smartautoclicker.feature.qstile.domain.QSTileRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class QSTileLauncherViewModel @Inject constructor(
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val qsTileRepository: QSTileRepository,
    private val permissionController: PermissionsController,
    private val smartRepository: IRepository,
    private val dumbRepository: DumbRepository,
    private val settingsRepository: SettingsRepository,
    private val appComponentsProvider: AppComponentsProvider,
) : ViewModel() {


    fun startPermissionFlowIfNeeded(activity: AppCompatActivity, onAllGranted: () -> Unit, onMandatoryDenied: () -> Unit) {
        permissionController.startPermissionsUiFlow(
            activity = activity,
            permissions = listOf(
                PermissionOverlay(),
                PermissionAccessibilityService(
                    componentName = appComponentsProvider.klickrServiceComponentName,
                    isServiceRunning = { qsTileRepository.isAccessibilityServiceStarted() },
                ),
                PermissionPostNotification(optional = true),
            ),
            onAllGranted = onAllGranted,
            onMandatoryDenied = onMandatoryDenied,
        )
    }

    fun startSmartScenario(resultCode: Int, data: Intent, scenarioId: Long) {
        viewModelScope.launch(ioDispatcher) {
            val scenario = smartRepository.getScenario(scenarioId) ?: return@launch
            qsTileRepository.startSmartScenario(resultCode, data, scenario)
        }
    }

    fun startDumbScenario(scenarioId: Long) {
        viewModelScope.launch(ioDispatcher) {
            val scenario = dumbRepository.getDumbScenario(scenarioId) ?: return@launch
            qsTileRepository.startDumbScenario(scenario)
        }
    }

    fun isEntireScreenCaptureForced(): Boolean =
        settingsRepository.isEntireScreenCaptureForced()
}