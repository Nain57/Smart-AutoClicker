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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.actions

import android.app.Application
import android.content.Context

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.baseui.overlays.dialog.DialogChoice
import com.buzbuz.smartautoclicker.billing.IBillingRepository
import com.buzbuz.smartautoclicker.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.domain.Repository
import com.buzbuz.smartautoclicker.domain.model.action.Action
import com.buzbuz.smartautoclicker.extensions.mapList
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.ActionDetails
import com.buzbuz.smartautoclicker.feature.scenario.config.ui.bindings.toActionDetails

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ActionsViewModel(application: Application) : AndroidViewModel(application) {

    /** The repository of the application. */
    private val repository: Repository = Repository.getRepository(application)
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository = IBillingRepository.getRepository(application)

    /** Currently configured event. */
    private val configuredEvent = editionRepository.editedEvent
        .filterNotNull()

    /** Tells if the limitation in action count have been reached. */
    val isActionLimitReached: Flow<Boolean> = billingRepository.isProModePurchased
        .combine(configuredEvent) { isProModePurchased, event ->
            !isProModePurchased && (event.actions.size >= ProModeAdvantage.Limitation.ACTION_COUNT_LIMIT.limit)
        }

    /** Tells if there is at least one action to copy. */
    val canCopyAction: Flow<Boolean> = repository.getAllActions()
        .map { it.isNotEmpty() }

    /** List of action details. */
    val actionDetails: Flow<List<Pair<Action, ActionDetails>>> = configuredEvent
        .map { it.actions }
        .mapList { action -> action to action.toActionDetails(application) }
    /** Type of actions to be displayed in the new action creation dialog. */
    val actionCreationItems: StateFlow<List<ActionTypeChoice>> = billingRepository.isProModePurchased
        .map { isProModePurchased ->
            buildList {
                add(ActionTypeChoice.Click)
                add(ActionTypeChoice.Swipe)
                add(ActionTypeChoice.Pause)
                add(ActionTypeChoice.Intent(isProModePurchased))
                add(ActionTypeChoice.ToggleEvent(isProModePurchased))
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList(),
        )

    /** Tells if the pro mode billing flow is being displayed. */
    val isBillingFlowDisplayed: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    /**
     * Create a new action with the default values from configuration.
     *
     * @param context the Android Context.
     * @param actionType the type of action to create.
     */
    fun createAction(context: Context, actionType: ActionTypeChoice): Action = when (actionType) {
        is ActionTypeChoice.Click -> editionRepository.createNewClick(context, from = null)
        is ActionTypeChoice.Swipe -> editionRepository.createNewSwipe(context, from = null)
        is ActionTypeChoice.Pause -> editionRepository.createNewPause(context, from = null)
        is ActionTypeChoice.Intent -> editionRepository.createNewIntent(context, from = null)
        is ActionTypeChoice.ToggleEvent -> editionRepository.createNewToggleEvent(context, from = null)
    }

    fun upsertAction(action: Action) {
        editionRepository.upsertActionToEditedEvent(action)
    }

    /**
     * Remove an action from the event.
     * @param action the action to be removed.
     */
    fun removeAction(action: Action) {
        editionRepository.removeActionFromEditedEvent(action)
    }

    /**
     * Update the priority of the actions.
     * @param actions the new actions order.
     */
    fun updateActionOrder(actions: List<Pair<Action, ActionDetails>>) =
        editionRepository.updateActionsOrder(actions.map { it.first })

    fun onActionCountReachedAddCopyClicked(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Limitation.ACTION_COUNT_LIMIT)
    }

    fun onProModeUnsubscribedActionClicked(context: Context, choice: ActionTypeChoice) {
        val feature = when (choice) {
            is ActionTypeChoice.Intent -> ProModeAdvantage.Feature.ACTION_TYPE_INTENT
            is ActionTypeChoice.ToggleEvent -> ProModeAdvantage.Feature.ACTION_TYPE_TOGGLE_EVENT
            else -> return
        }

        billingRepository.startBillingActivity(context, feature)
    }
}

/** Choices for the action type selection dialog. */
sealed class ActionTypeChoice(
    title: Int,
    description: Int,
    iconId: Int?,
    enabled: Boolean,
): DialogChoice(
    title = title,
    description = description,
    iconId = iconId,
    disabledIconId = R.drawable.ic_pro_small,
    enabled = enabled,
) {
    /** Click Action choice. */
    object Click : ActionTypeChoice(
        R.string.item_title_click,
        R.string.item_desc_click,
        R.drawable.ic_click,
        enabled = true,
    )
    /** Swipe Action choice. */
    object Swipe : ActionTypeChoice(
        R.string.item_title_swipe,
        R.string.item_desc_swipe,
        R.drawable.ic_swipe,
        enabled = true,
    )
    /** Pause Action choice. */
    object Pause : ActionTypeChoice(
        R.string.item_title_pause,
        R.string.item_desc_pause,
        R.drawable.ic_wait,
        enabled = true,
    )
    /** Intent Action choice. */
    class Intent(enabled: Boolean) : ActionTypeChoice(
        R.string.item_title_intent,
        R.string.item_desc_intent,
        R.drawable.ic_intent,
        enabled = enabled,
    )
    /** Toggle Event Action choice. */
    class ToggleEvent(enabled: Boolean) : ActionTypeChoice(
        R.string.item_title_toggle_event,
        R.string.item_desc_toggle_event,
        R.drawable.ic_toggle_event,
        enabled = enabled,
    )
}