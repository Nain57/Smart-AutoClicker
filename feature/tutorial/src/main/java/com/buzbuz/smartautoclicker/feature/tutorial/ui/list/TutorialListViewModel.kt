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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.data.AppComponentsProvider
import com.buzbuz.smartautoclicker.core.common.accessibility.domain.LocalAccessibilityServiceConnection
import com.buzbuz.smartautoclicker.core.common.permissions.PermissionsController
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionAccessibilityService
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionOverlay
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.TutorialRepository
import com.buzbuz.smartautoclicker.core.settings.domain.SettingsRepository
import com.buzbuz.smartautoclicker.feature.tutorial.data.mapping.toTutorialItem
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategory
import com.buzbuz.smartautoclicker.feature.tutorial.domain.GetTutorialCategoryUseCase
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategoryUiItems
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialCategoryUiState

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TutorialListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appComponentsProvider: AppComponentsProvider,
    private val accessibilityServiceConnection: LocalAccessibilityServiceConnection,
    private val permissionsController: PermissionsController,
    private val tutorialRepository: TutorialRepository,
    getTutorialCategoryUseCase: GetTutorialCategoryUseCase,
) : ViewModel() {

    private val categoryType: TutorialCategory.Type =
        TutorialListFragmentArgs.fromSavedStateHandle(savedStateHandle).categoryType

    val uiState: StateFlow<TutorialCategoryUiState> =
        getTutorialCategoryUseCase(categoryType)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), TutorialCategoryUiState.Loading)

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

    fun startTutorial(item: TutorialCategoryUiItems.Item.Tutorial, resultCode: Int, data: Intent) {
        tutorialRepository.startTutorial(item.type.toTutorialItem().getTutorial(), resultCode, data)
    }

    fun stopTutorial() {
        tutorialRepository.stopTutorial()
    }
}
