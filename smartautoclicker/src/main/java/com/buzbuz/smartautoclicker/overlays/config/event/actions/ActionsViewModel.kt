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
package com.buzbuz.smartautoclicker.overlays.config.event.actions

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.R
import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.extensions.mapList
import com.buzbuz.smartautoclicker.overlays.base.dialog.DialogChoice
import com.buzbuz.smartautoclicker.overlays.base.bindings.ActionDetails
import com.buzbuz.smartautoclicker.overlays.base.bindings.toActionDetails
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultClick
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultIntent
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultPause
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultSwipe
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultToggleEvent
import com.buzbuz.smartautoclicker.overlays.config.EditionRepository

import kotlinx.coroutines.flow.*

class ActionsViewModel(application: Application) : AndroidViewModel(application) {

    /** The repository of the application. */
    private val repository: Repository = Repository.getRepository(application)
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)

    /** Currently configured event. */
    private val configuredEvent = editionRepository.configuredEvent
        .filterNotNull()

    /** Tells if there is at least one action to copy. */
    val canCopyAction: Flow<Boolean> = repository.getAllActions()
        .map { it.isNotEmpty() }

    /** List of [actions]. */
    val actions: StateFlow<List<Action>> = configuredEvent
        .map { it.event.actions ?: emptyList() }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    /** List of action details. */
    val actionDetails: StateFlow<List<ActionDetails>> = actions
        .mapList { action -> action.toActionDetails(application) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    /**
     * Create a new action with the default values from configuration.
     *
     * @param context the Android Context.
     * @param actionType the type of action to create.
     */
    fun createAction(context: Context, actionType: ActionTypeChoice): Action {
        editionRepository.configuredEvent.value?.let { conf ->

            return when (actionType) {
                is ActionTypeChoice.Click -> newDefaultClick(context, conf.event.id)
                is ActionTypeChoice.Swipe -> newDefaultSwipe(context, conf.event.id)
                is ActionTypeChoice.Pause -> newDefaultPause(context, conf.event.id)
                is ActionTypeChoice.Intent -> newDefaultIntent(context, conf.event.id)
                is ActionTypeChoice.ToggleEvent -> newDefaultToggleEvent(context, conf.event.id)
            }

        } ?: throw IllegalStateException("Can't create an action, event is null!")
    }

    fun addUpdateAction(action: Action, index: Int) {
        if (index != -1) editionRepository.updateActionFromEditedEvent(action, index)
        else editionRepository.addActionToEditedEvent(action)
    }

    /**
     * Remove an action from the event.
     * @param action the action to be removed.
     */
    fun removeAction(action: Action) = editionRepository.removeActionFromEditedEvent(action)

    /**
     * Update the priority of the actions.
     * @param actions the new actions order.
     */
    fun updateActionOrder(actions: List<ActionDetails>) = editionRepository
        .updateActionOrder(actions.map { it.action })
}

/** Choices for the action type selection dialog. */
sealed class ActionTypeChoice(title: Int, description: Int, iconId: Int?): DialogChoice(title, description, iconId) {
    /** Click Action choice. */
    object Click : ActionTypeChoice(
        R.string.item_title_click,
        R.string.item_desc_click,
        R.drawable.ic_click,
    )
    /** Swipe Action choice. */
    object Swipe : ActionTypeChoice(
        R.string.item_title_swipe,
        R.string.item_desc_swipe,
        R.drawable.ic_swipe,
    )
    /** Pause Action choice. */
    object Pause : ActionTypeChoice(
        R.string.item_title_pause,
        R.string.item_desc_pause,
        R.drawable.ic_wait,
    )
    /** Intent Action choice. */
    object Intent : ActionTypeChoice(
        R.string.item_title_intent,
        R.string.item_desc_intent,
        R.drawable.ic_intent,
    )
    /** Toggle Event Action choice. */
    object ToggleEvent : ActionTypeChoice(
        R.string.item_title_toggle_event,
        R.string.item_desc_toggle_event,
        R.drawable.ic_toggle_event,
    )
}