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

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.IntentExtra
import com.buzbuz.smartautoclicker.overlays.eventconfig.action.ActionModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

/**
 * Allow to observe/edit the values a intent action.
 *
 * @param viewModelScope the scope for the view model holding this model.
 * @param configuredAction the flow on the edited action.
 */
class IntentConfigModel(
    private val viewModelScope: CoroutineScope,
    val configuredAction: MutableStateFlow<Action?>,
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
        // Nothing to do
    }

    /** True if the intent creation mode is advanced, false if not. */
    val isAdvanced: Flow<Boolean> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.isAdvanced ?: false }
        .distinctUntilChanged()
    /* The intent action. */
    val action: Flow<String?> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.intentAction }
        .take(1)
    /** The flags for this intent. */
    val flags: Flow<String?> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.flags.toString() }
        .take(1)
    /** The component name for the intent. */
    val componentName: Flow<String?> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.componentName?.flattenToString() }
        .take(1)
    /** True if this intent is a broadcast, false for a start activity. */
    val isBroadcast: Flow<Boolean> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { it.isBroadcast ?: false }

    /** The list of extra items to be displayed. */
    val extras: Flow<List<ExtraListItem>> = configuredAction
        .filterIsInstance<Action.Intent>()
        .map { intent ->
            buildList {
                intent.extras?.forEach { extra ->
                    val lastDotIndex = extra.key!!.lastIndexOf('.', 0)
                    add(
                        ExtraListItem.ExtraItem(
                            extra = extra,
                            name = if (lastDotIndex == -1) extra.key!! else extra.key!!.substring(lastDotIndex),
                            value = extra.value.toString()
                        )
                    )
                }

                add(ExtraListItem.AddExtraItem)
            }
        }

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
                val isAdvanced =  !(intent.isAdvanced ?: false)
                configuredAction.value = intent.copy(
                    isAdvanced = isAdvanced,
                    isBroadcast = if(!isAdvanced) false else intent.isBroadcast
                )
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

    /** Set the action for the intent. */
    fun setIntentAction(action: String) {
        (configuredAction.value as Action.Intent).let { intent ->
            viewModelScope.launch {
                configuredAction.value = intent.copy(intentAction = action)
            }
        }
    }

    /** Set the action for the intent. */
    fun setFlagsAction(flags: Int?) {
        (configuredAction.value as Action.Intent).let { intent ->
            viewModelScope.launch {
                configuredAction.value = intent.copy(flags = flags)
            }
        }
    }

    /** Set the component name for the intent. */
    fun setComponentName(componentName: String) {
        (configuredAction.value as Action.Intent).let { intent ->
            viewModelScope.launch {
                configuredAction.value = intent.copy(componentName = ComponentName.unflattenFromString(componentName))
            }
        }
    }

    /** Toggle between a broadcast and start activity. */
    fun toggleIsBroadcast() {
        (configuredAction.value as Action.Intent).let { intent ->
            viewModelScope.launch {
                configuredAction.value = intent.copy(isBroadcast = !(intent.isBroadcast ?: false))
            }
        }
    }

    /** @return creates a new extra for this intent. */
    fun getNewExtra() = IntentExtra(0L, configuredAction.value!!.id, null, null)

    /**
     * Add a new extra to the configured intent.
     * @param extra the new extra to add.
     */
    fun addNewExtra(extra: IntentExtra<out Any>) {
        (configuredAction.value as Action.Intent).let { intent ->
            viewModelScope.launch {
                val newList = intent.extras?.toMutableList() ?: mutableListOf()
                newList.add(extra)
                configuredAction.value = intent.copy(extras = newList)
            }
        }
    }

    /**
     * Update an extra in the configured intent.
     * @param extra the extra to update.
     * @param index the index of the extra in the extra list.
     */
    fun updateExtra(extra: IntentExtra<out Any>, index: Int) {
        (configuredAction.value as Action.Intent).let { intent ->
            viewModelScope.launch {
                val newList = intent.extras?.toMutableList() ?: return@launch
                newList[index] = extra
                configuredAction.value = intent.copy(extras = newList)
            }
        }
    }

    /**
     * Delete an extra in the configured intent.
     * @param index the index of the extra in the extra list.
     */
    fun deleteExtra(index: Int) {
        (configuredAction.value as Action.Intent).let { intent ->
            viewModelScope.launch {
                val newList = intent.extras?.toMutableList() ?: return@launch
                newList.removeAt(index)
                configuredAction.value = intent.copy(extras = newList)
            }
        }
    }
}

/**
 * Information about an activity to be started by the intent.
 *
 * @param componentName the Android component name of the activity.
 * @param name the name of the activity.
 * @param icon the icon of the activity.
 */
data class ActivityDisplayInfo(
    val componentName: ComponentName,
    val name: String,
    val icon: Drawable,
)

/** Items displayed in the extra list. */
sealed class ExtraListItem {
    /** The add extra item. */
    object AddExtraItem : ExtraListItem()
    /** Item representing an intent extra. */
    data class ExtraItem(val extra: IntentExtra<out Any>, val name: String, val value: String) : ExtraListItem()
}

/**
 * Get the activity display information from this resolve info, if possible.
 * @param packageManager the Android package manager to fetch the information from.
 * @return activity display information, or null if this resolve info is invalid for activity.
 */
fun ResolveInfo.getActivityDisplayInfo(packageManager: PackageManager): ActivityDisplayInfo? =
    activityInfo?.let { actInfo ->
        ActivityDisplayInfo(
            ComponentName(actInfo.packageName, actInfo.name),
            actInfo.loadLabel(packageManager).toString(),
            actInfo.loadIcon(packageManager)
        )
    }
