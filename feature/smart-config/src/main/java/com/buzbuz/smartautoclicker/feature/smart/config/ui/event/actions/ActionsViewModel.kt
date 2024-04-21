/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.event.actions

import android.content.Context
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.ui.overlays.dialog.DialogChoice
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.ActionDetails
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.bindings.toActionDetails

import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class ActionsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val editionRepository: EditionRepository,
    private val billingRepository: IBillingRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    /** Currently configured actions. */
    private val configuredActions = editionRepository.editionState.editedEventActionsState

    /** Tells if the limitation in action count have been reached. */
    val isActionLimitReached: Flow<Boolean> = billingRepository.isProModePurchased
        .combine(configuredActions) { isProModePurchased, actions ->
            !isProModePurchased && ((actions.value?.size ?: 0) >= ProModeAdvantage.Limitation.ACTION_COUNT_LIMIT.limit)
        }

    /** Tells if there is at least one action to copy. */
    val canCopyAction: Flow<Boolean> = editionRepository.editionState.canCopyActions

    /** List of action details. */
    val actionDetails: Flow<List<Pair<Action, ActionDetails>>> = configuredActions
        .map { actions ->
            actions.value?.mapIndexed { index, action ->
                action to action.toActionDetails(context, !actions.itemValidity[index])
            } ?: emptyList()
        }
    /** Type of actions to be displayed in the new action creation dialog. */
    val actionCreationItems: StateFlow<List<ActionTypeChoice>> = billingRepository.isProModePurchased
        .map { isProModePurchased ->
            buildList {
                add(ActionTypeChoice.Click)
                add(ActionTypeChoice.Swipe)
                add(ActionTypeChoice.Pause)
                add(ActionTypeChoice.ChangeCounter(isProModePurchased))
                add(ActionTypeChoice.ToggleEvent(isProModePurchased))
                add(ActionTypeChoice.Intent(isProModePurchased))
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
        is ActionTypeChoice.Click -> editionRepository.editedItemsBuilder.createNewClick(context)
        is ActionTypeChoice.Swipe -> editionRepository.editedItemsBuilder.createNewSwipe(context)
        is ActionTypeChoice.Pause -> editionRepository.editedItemsBuilder.createNewPause(context)
        is ActionTypeChoice.Intent -> editionRepository.editedItemsBuilder.createNewIntent(context)
        is ActionTypeChoice.ToggleEvent -> editionRepository.editedItemsBuilder.createNewToggleEvent(context)
        is ActionTypeChoice.ChangeCounter -> editionRepository.editedItemsBuilder.createNewChangeCounter(context)
    }

    /**
     * Get a new action based on the provided one.
     * @param action the item containing the action to copy.
     */
    fun createNewActionFrom(action: Action): Action =
        editionRepository.editedItemsBuilder.createNewActionFrom(action)

    fun startActionEdition(action: Action) = editionRepository.startActionEdition(action)

    fun upsertEditedAction() = editionRepository.upsertEditedAction()

    fun removeEditedAction() = editionRepository.deleteEditedAction()

    fun dismissEditedAction() = editionRepository.stopActionEdition()

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


    fun monitorCreateActionView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_ACTION, view)
    }

    fun monitorFirstActionView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_ACTION, view)
    }

    fun stopFirstActionViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_ACTION)
    }

    fun stopAllViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_ACTION)
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_ACTION)
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
    data object Click : ActionTypeChoice(
        R.string.item_title_click,
        R.string.item_desc_click,
        R.drawable.ic_click,
        enabled = true,
    )
    /** Swipe Action choice. */
    data object Swipe : ActionTypeChoice(
        R.string.item_title_swipe,
        R.string.item_desc_swipe,
        R.drawable.ic_swipe,
        enabled = true,
    )
    /** Pause Action choice. */
    data object Pause : ActionTypeChoice(
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

    /** Change counter Action choice. */
    class ChangeCounter(enabled: Boolean) : ActionTypeChoice(
        R.string.item_title_change_counter,
        R.string.item_desc_change_counter,
        R.drawable.ic_change_counter,
        enabled = enabled,
    )
}