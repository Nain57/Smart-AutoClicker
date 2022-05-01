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
package com.buzbuz.smartautoclicker.overlays.eventconfig.action.intent

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

import com.buzbuz.smartautoclicker.database.domain.Action
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ActionModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Allow to observe/edit the values a intent action.
 *
 * @param viewModelScope the scope for the view model holding this model.
 * @param configuredAction the flow on the edited action.
 */
class IntentConfigModel(
    private val viewModelScope: CoroutineScope,
    private val configuredAction: MutableStateFlow<Action?>,
    private val packageManager: PackageManager,
) : ActionModel() {

    override val isValidAction: Flow<Boolean> = configuredAction
        .map { action ->
            action is Action.Intent
                    && !action.name.isNullOrEmpty()
                    && action.isAdvanced != null && action.intentAction != null && action.flags != null
                    && (action.isBroadcast == true || (action.isBroadcast == false && action.componentName != null))
        }

    override fun saveLastConfig(eventConfigPrefs: SharedPreferences) {
        configuredAction.value?.let { action ->
            if (action is Action.Intent) {
                // TODO to be implemented
            }
        }
    }

    /** True if the intent creation mode is advanced, false if not. */
    val isAdvanced: Flow<Boolean> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.isAdvanced ?: false }
    /* The intent action. */
    val action: Flow<String?> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.intentAction }
    /** The flags for this intent. */
    val flags: Flow<Int?> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.flags }
    /** The component name for the intent. */
    val componentName: Flow<ComponentName?> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.componentName }
    /** True if this intent is a broadcast, false for a start activity. */
    val isBroadcast: Flow<Boolean> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.isBroadcast ?: false }

    /** Name and icon of the selected application in simple edition mode. */
    val activityInfo: Flow<ActivityDisplayInfo?> = configuredAction
        .filterIsInstance<Action.Intent>()
        .filter { it.isAdvanced == false }
        .map { intent ->
            if (intent.componentName == null) return@map null

            packageManager.resolveActivity(Intent(intent.intentAction).setComponent(intent.componentName!!), 0)
                ?.getActivityDisplayInfo(packageManager)
        }

    /** Toggle between Advanced and Simple edition mode. */
    fun toggleIsAdvanced() {
        (configuredAction.value as Action.Intent).let { intent ->
            viewModelScope.launch {
                configuredAction.value = intent.copy(isAdvanced = !(intent.isAdvanced ?: false))
            }
        }
    }

    /**
     * Set the activity selected by the user in simple mode.
     * This will change the component name, but also all other parameters required for a default start activity.
     *
     * @param componentName component name of the selected activity.
     */
    fun setActivitySelected(componentName: ComponentName) {
        (configuredAction.value as Action.Intent).let { intent ->
            configuredAction.value = intent.copy(
                isBroadcast = false,
                intentAction = Intent.ACTION_MAIN,
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                componentName = componentName,
            )
        }
    }

    /** Set the component name for the intent. */
    fun setComponentName(componentName: ComponentName) {
        (configuredAction.value as Action.Intent).let { intent ->
            viewModelScope.launch {
                configuredAction.value = intent.copy(componentName = componentName)
            }
        }
    }
}

data class ActivityDisplayInfo(
    val componentName: ComponentName,
    val name: String,
    val icon: Drawable,
)

fun ResolveInfo.getActivityDisplayInfo(packageManager: PackageManager): ActivityDisplayInfo? =
    activityInfo?.let { actInfo ->
        ActivityDisplayInfo(
            ComponentName(actInfo.packageName, actInfo.name),
            actInfo.loadLabel(packageManager).toString(),
            actInfo.loadIcon(packageManager)
        )
    }
