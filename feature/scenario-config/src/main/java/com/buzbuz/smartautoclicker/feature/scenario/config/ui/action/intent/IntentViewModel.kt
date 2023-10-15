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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.intent

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.base.extensions.resolveActivityCompat
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.IntentExtra
import com.buzbuz.smartautoclicker.core.ui.bindings.dropdown.DropdownItem
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.putIntentIsAdvancedConfig
import kotlinx.coroutines.FlowPreview

import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
class IntentViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The action being configured by the user. */
    private val configuredIntent = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Action.Intent>()

    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = application.getEventConfigPreferences()
    /** The Android package manager. */
    private val packageManager: PackageManager = application.packageManager

    /** Tells if the user is currently editing an action. If that's not the case, dialog should be closed. */
    val isEditingAction: Flow<Boolean> = editionRepository.isEditingAction
        .distinctUntilChanged()
        .debounce(1000)

    /** The name of the pause. */
    val name: Flow<String?> = configuredIntent
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredIntent.map { it.name?.isEmpty() ?: true }

    /* The intent action. */
    val action: Flow<String?> = configuredIntent
        .map { it.intentAction }
        .take(1)
    /** Tells if the intent action is valid or not. */
    val actionError: Flow<Boolean> = configuredIntent.map { it.intentAction?.isEmpty() ?: true }

    /** The flags for this intent. */
    val flags: Flow<String> = configuredIntent
        .map { it.flags?.toString() ?: "" }
        .take(1)

    /** The component name for the intent. */
    val componentName: Flow<String?> = configuredIntent
        .map { it.componentName?.flattenToString() }
        .take(1)
    /** Tells if the intent component name is valid or not. */
    val componentNameError: Flow<Boolean> = configuredIntent.map { intent ->
        intent.isBroadcast == false && intent.componentName == null
    }

    private val sendingTypeActivity = DropdownItem(title = R.string.dropdown_item_title_intent_sending_type_activity)
    private val sendingTypeBroadcast = DropdownItem(title = R.string.dropdown_item_title_intent_sending_type_broadcast)
    /** Sending types choices for the dropdown field. */
    val sendingTypeItems = listOf(sendingTypeActivity, sendingTypeBroadcast)
    /** Current choice for the sending type dropdown field. */
    val isBroadcast: Flow<DropdownItem> = configuredIntent
        .map {
            when (it.isBroadcast) {
                true -> sendingTypeBroadcast
                false -> sendingTypeActivity
                null -> null
            }
        }
        .filterNotNull()

    /** The list of extra items to be displayed. */
    val extras: Flow<List<ExtraListItem>> = editionRepository.editionState.editedActionIntentExtrasState
        .map { intentExtra ->
            buildList {
                intentExtra.value?.forEach { extra ->
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
        .filter { it.isAdvanced == false }
        .map { intent ->
            if (intent.componentName == null) return@map null

            packageManager.resolveActivityCompat(Intent(intent.intentAction).setComponent(intent.componentName!!), 0)
                ?.getActivityDisplayInfo(packageManager)
        }

    /** Tells if the configured intent is valid and can be saved. */
    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }

    fun isAdvanced(): Boolean = editionRepository.editionState.getEditedAction<Action.Intent>()?.isAdvanced ?: false

    /**
     * Set the name of the intent.
     * @param name the new name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<Action.Intent>()?.let { intent ->
            editionRepository.updateEditedAction(intent.copy(name = "" + name))
        }
    }

    /** Set the configuration mode. */
    fun setIsAdvancedConfiguration(isAdvanced: Boolean) {
        editionRepository.editionState.getEditedAction<Action.Intent>()?.let { intent ->
            editionRepository.updateEditedAction(
                intent.copy(
                    isAdvanced = isAdvanced,
                    isBroadcast = if(!isAdvanced) false else intent.isBroadcast
                )
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
        editionRepository.editionState.getEditedAction<Action.Intent>()?.let { intent ->
            editionRepository.updateEditedAction(
                intent.copy(
                    isBroadcast = false,
                    intentAction = Intent.ACTION_MAIN,
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP,
                    componentName = componentName,
                )
            )
        }
    }

    /** Set the action for the intent. */
    fun setIntentAction(action: String) {
        editionRepository.editionState.getEditedAction<Action.Intent>()?.let { intent ->
            editionRepository.updateEditedAction(intent.copy(intentAction = action))
        }
    }

    /** Set the action for the intent. */
    fun setIntentFlags(flags: Int?) {
        editionRepository.editionState.getEditedAction<Action.Intent>()?.let { intent ->
            editionRepository.updateEditedAction(intent.copy(flags = flags))
        }
    }

    /** Set the component name for the intent. */
    fun setComponentName(componentName: String) {
        editionRepository.editionState.getEditedAction<Action.Intent>()?.let { intent ->
            editionRepository.updateEditedAction(
                intent.copy(componentName = ComponentName.unflattenFromString(componentName))
            )
        }
    }

    /** Set the sending type. of the intent */
    fun setSendingType(newType: DropdownItem) {
        editionRepository.editionState.getEditedAction<Action.Intent>()?.let { intent ->
            val isBroadcast = when (newType) {
                sendingTypeBroadcast -> true
                sendingTypeActivity -> false
                else -> return
            }

            editionRepository.updateEditedAction(intent.copy(isBroadcast = isBroadcast))
        }
    }

    /** @return creates a new extra for this intent. */
    fun createNewExtra(): IntentExtra<Any> =
        editionRepository.editedItemsBuilder.createNewIntentExtra()

    /** Start the edition of an intent extra. */
    fun startIntentExtraEdition(extra: IntentExtra<out Any>) = editionRepository.startIntentExtraEdition(extra)

    /** Add or update an extra. If the extra id is unset, it will be added. If not, updated. */
    fun saveIntentExtraEdition() = editionRepository.upsertEditedIntentExtra()

    /** Delete an extra. */
    fun deleteIntentExtraEvent() = editionRepository.deleteEditedIntentExtra()

    /** Drop all changes made to the currently edited extra. */
    fun dismissIntentExtraEvent() = editionRepository.stopIntentExtraEdition()

    fun saveLastConfig() {
        editionRepository.editionState.getEditedAction<Action.Intent>()?.let { intent ->
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