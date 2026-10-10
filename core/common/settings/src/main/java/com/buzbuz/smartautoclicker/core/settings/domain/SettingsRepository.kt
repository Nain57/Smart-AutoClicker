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
package com.buzbuz.smartautoclicker.core.settings.domain

import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortSettings
import com.buzbuz.smartautoclicker.core.settings.domain.model.ScenarioSortType
import kotlinx.coroutines.flow.Flow

/** Keep track of the app user settings. */
interface SettingsRepository {

    /** Allows to display the action overlay as a list dialog. False by default. */
    val isLegacyActionUiEnabledFlow: Flow<Boolean>
    fun isLegacyActionUiEnabled(): Boolean
    fun toggleLegacyActionUi()

    /** Allows to display the buttons in the notification as text. False by default. */
    val isLegacyNotificationUiEnabledFlow: Flow<Boolean>
    fun isLegacyNotificationUiEnabled(): Boolean
    fun toggleLegacyNotificationUi()

    /** Allows to force the MediaProjection permission dialog to use "Entire Screen" option by default. False by default. */
    val isEntireScreenCaptureForcedFlow: Flow<Boolean>
    fun isEntireScreenCaptureForced(): Boolean
    fun toggleForceEntireScreenCapture()

    /** Allows to show/hide the filters in the scenario list screen. True by default.*/
    val isFilterScenarioUiEnabledFlow: Flow<Boolean>
    fun toggleFilterScenarioUi()

    /** Allows to inject an unblock gesture during the scenario as a workaround for some devices. */
    val isInputBlockWorkaroundEnabledFlow: Flow<Boolean>
    fun isInputBlockWorkaroundEnabled(): Boolean
    fun toggleInputBlockWorkaround()

    /** Allows to show the app overlays on the lockscreen. */
    val showOnLockScreenFlow: Flow<Boolean>
    fun showOnLockScreen(): Boolean
    fun toggleShowOnLockScreenFlow()


    val scenarioSortSettings: Flow<ScenarioSortSettings>
    fun setScenarioSortType(type: ScenarioSortType)
    fun setScenarioSortOrder(invertSortOrder: Boolean)
    fun setScenarioSortShowDumb(show: Boolean)
    fun setScenarioSortShowSmart(show: Boolean)


}