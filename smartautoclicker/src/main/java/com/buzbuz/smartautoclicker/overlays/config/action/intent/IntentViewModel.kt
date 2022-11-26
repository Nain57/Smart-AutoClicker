/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.action.intent

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.IntentExtra
import com.buzbuz.smartautoclicker.extensions.resolveActivityCompat
import com.buzbuz.smartautoclicker.overlays.base.bindings.DropdownItem
import com.buzbuz.smartautoclicker.overlays.base.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.overlays.base.utils.putIntentIsAdvancedConfig

import kotlinx.coroutines.flow.*

class IntentViewModel(application: Application) : AndroidViewModel(application) {

    /** The action being configured by the user. Defined using [setConfiguredIntent]. */
    private val configuredIntent = MutableStateFlow<Action.Intent?>(null)
    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = application.getEventConfigPreferences()
    /** The Android package manager. */
    private val packageManager: PackageManager = application.packageManager

    /** The name of the pause. */
    val name: Flow<String?> = configuredIntent
        .filterNotNull()
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredIntent.map { it?.name?.isEmpty() ?: true }

    /* The intent action. */
    val action: Flow<String?> = configuredIntent
        .filterNotNull()
        .map { it.intentAction }
        .take(1)
    /** Tells if the intent action is valid or not. */
    val actionError: Flow<Boolean> = configuredIntent.map { it?.intentAction?.isEmpty() ?: true }

    /** The flags for this intent. */
    val flags: Flow<String> = configuredIntent
        .filterNotNull()
        .map { it.flags.toString() }
        .take(1)

    /** The component name for the intent. */
    val componentName: Flow<String?> = configuredIntent
        .filterNotNull()
        .map { it.componentName?.flattenToString() }
        .take(1)
    /** Tells if the intent component name is valid or not. */
    val componentNameError: Flow<Boolean> = configuredIntent.map { intent ->
        intent ?: return@map true
        intent.isBroadcast == false && intent.componentName == null
    }

    private val sendingTypeActivity = DropdownItem(title = R.string.dropdown_item_title_intent_sending_type_activity)
    private val sendingTypeBroadcast = DropdownItem(title = R.string.dropdown_item_title_intent_sending_type_broadcast)
    /** Sending types choices for the dropdown field. */
    val sendingTypeItems = listOf(sendingTypeActivity, sendingTypeBroadcast)
    /** Current choice for the sending type dropdown field. */
    val isBroadcast: Flow<DropdownItem> = configuredIntent
        .map {
            when (it?.isBroadcast) {
                true -> sendingTypeBroadcast
                false -> sendingTypeActivity
                null -> null
            }
        }
        .filterNotNull()

    /** The list of extra items to be displayed. */
    val extras: Flow<List<ExtraListItem>> = configuredIntent
        .filterNotNull()
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
    val activityInfo: Flow<ActivityDisplayInfo?> = configuredIntent
        .filterNotNull()
        .filter { it.isAdvanced == false }
        .map { intent ->
            if (intent.componentName == null) return@map null

            packageManager.resolveActivityCompat(Intent(intent.intentAction).setComponent(intent.componentName!!), 0)
                ?.getActivityDisplayInfo(packageManager)
        }

    /** Tells if the configured intent is valid and can be saved. */
    val isValidAction: Flow<Boolean> = configuredIntent
        .map { intent ->
            intent != null
                    && !intent.name.isNullOrEmpty()
                    && intent.isAdvanced != null && intent.intentAction != null && intent.flags != null
                    && (intent.isBroadcast == true || (intent.isBroadcast == false && intent.componentName != null))
        }

    /**
     * Set the configured intent.
     * This will update all values represented by this view model.
     *
     * @param intent the intent to configure.
     */
    fun setConfiguredIntent(intent: Action.Intent) {
        configuredIntent.value = intent.deepCopy()
    }

    /** @return the intent containing all user changes. */
    fun getConfiguredIntent(): Action.Intent =
        configuredIntent.value ?: throw IllegalStateException("Can't get the configured intent, none were defined.")

    /**
     * Set the name of the intent.
     * @param name the new name.
     */
    fun setName(name: String) {
        configuredIntent.value?.let { intent ->
            configuredIntent.value = intent.copy(name = "" + name)
        }
    }

    /** Set the configuration mode. */
    fun setIsAdvancedConfiguration(isAdvanced: Boolean) {
        configuredIntent.value?.let { intent ->
            configuredIntent.value = intent.copy(
                isAdvanced = isAdvanced,
                isBroadcast = if(!isAdvanced) false else intent.isBroadcast
            )
        }
    }

    /**
     * Set the activity selected by the user in simple mode.
     * This will change the component name, but also all other parameters required for a default start activity.
     *
     * @param componentName component name of the selected activity.
     */
    fun setActivitySelected(componentName: ComponentName) {
        configuredIntent.value?.let { intent ->
            configuredIntent.value = intent.copy(
                isBroadcast = false,
                intentAction = Intent.ACTION_MAIN,
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                componentName = componentName,
            )
        }
    }

    /** Set the action for the intent. */
    fun setIntentAction(action: String) {
        configuredIntent.value?.let { intent ->
            configuredIntent.value = intent.copy(intentAction = action)
        }
    }

    /** Set the action for the intent. */
    fun setIntentFlags(flags: Int?) {
        configuredIntent.value?.let { intent ->
            configuredIntent.value = intent.copy(flags = flags)
        }
    }

    /** Set the component name for the intent. */
    fun setComponentName(componentName: String) {
        configuredIntent.value?.let { intent ->
            configuredIntent.value = intent.copy(componentName = ComponentName.unflattenFromString(componentName))
        }
    }

    /** Set the sending type. of the intent */
    fun setSendingType(newType: DropdownItem) {
        configuredIntent.value?.let { intent ->
            val isBroadcast = when (newType) {
                sendingTypeBroadcast -> true
                sendingTypeActivity -> false
                else -> return
            }
            configuredIntent.value = intent.copy(isBroadcast = isBroadcast)
        }
    }

    /** @return creates a new extra for this intent. */
    fun getNewExtra() = configuredIntent.value?.let { IntentExtra(0L, it.id, null, null) }
        ?: throw IllegalStateException("Can't create new extra, no configured intent defined.")


    fun addUpdateExtra(extra: IntentExtra<out Any>, index: Int) {
        if (index != -1) updateExtra(extra, index)
        else addNewExtra(extra)
    }

    /**
     * Add a new extra to the configured intent.
     * @param extra the new extra to add.
     */
    private fun addNewExtra(extra: IntentExtra<out Any>) {
        configuredIntent.value?.let { intent ->
            val newList = intent.extras?.toMutableList() ?: mutableListOf()
            newList.add(extra)
            configuredIntent.value = intent.copy(extras = newList)
        }
    }

    /**
     * Update an extra in the configured intent.
     * @param extra the extra to update.
     * @param index the index of the extra in the extra list.
     */
    private fun updateExtra(extra: IntentExtra<out Any>, index: Int) {
        configuredIntent.value?.let { intent ->
            val newList = intent.extras?.toMutableList() ?: return
            newList[index] = extra
            configuredIntent.value = intent.copy(extras = newList)
        }
    }

    /**
     * Delete an extra in the configured intent.
     * @param index the index of the extra in the extra list.
     */
    fun deleteExtra(index: Int) {
        configuredIntent.value?.let { intent ->
            val newList = intent.extras?.toMutableList() ?: return
            newList.removeAt(index)
            configuredIntent.value = intent.copy(extras = newList)
        }
    }

    fun saveLastConfig() {
        configuredIntent.value?.let { intent ->
            sharedPreferences.edit().putIntentIsAdvancedConfig(intent.isAdvanced == true).apply()
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