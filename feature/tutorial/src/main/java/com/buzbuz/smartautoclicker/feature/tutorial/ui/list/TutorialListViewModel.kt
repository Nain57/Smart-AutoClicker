/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.tutorial.ui.list

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.common.accessibility.domain.LocalAccessibilityServiceConnection
import com.buzbuz.smartautoclicker.core.common.permissions.PermissionsController
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionAccessibilityService
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionOverlay
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.TutorialInfo

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class TutorialListViewModel @Inject constructor(
    private val appComponentsProvider: AppComponentsProvider,
    private val accessibilityServiceConnection: LocalAccessibilityServiceConnection,
    private val permissionsController: PermissionsController,
    private val settingsRepository: SettingsRepository,
    private val tutorialRepository: TutorialRepository,
) : ViewModel() {

    val items: Flow<List<TutorialItem>> = tutorialRepository.tutorialList
        .map { tutorials -> tutorials.map { tutorial -> tutorial.toItem() } }


    fun isEntireScreenCaptureForced(): Boolean =
        settingsRepository.isEntireScreenCaptureForced()

    fun startPermissionFlowIfNeeded(activity: AppCompatActivity, onAllGranted: () -> Unit) {
        permissionsController.startPermissionsUiFlow(
            activity = activity,
            permissions = listOf(
                PermissionOverlay(),
                PermissionAccessibilityService(
                    componentName = appComponentsProvider.klickrServiceComponentName,
                    isServiceRunning = { accessibilityServiceConnection.isServiceStarted() },
                ),
                PermissionPostNotification(optional = true),
            ),
            onAllGranted = onAllGranted,
        )
    }

    fun startTutorial(tutorialId: String, resultCode: Int, data: Intent) {
        val tutorial = tutorialRepository.getTutorial(tutorialId) ?: return
        tutorialRepository.startTutorial(tutorial, resultCode, data)
    }

    fun stopTutorial() {
        tutorialRepository.stopTutorial()
    }
    private fun TutorialInfo.toItem(): TutorialItem =
        TutorialItem(
            nameResId = nameResId,
            descResId = descResId,
            tutorialId = id,
        )
}
