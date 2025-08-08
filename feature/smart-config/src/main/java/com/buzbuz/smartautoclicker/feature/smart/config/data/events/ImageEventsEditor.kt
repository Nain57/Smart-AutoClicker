
package com.buzbuz.smartautoclicker.feature.smart.config.data.events

import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.OR
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.condition.ImageCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

import kotlinx.coroutines.flow.StateFlow

internal class ImageEventsEditor(
    onDeleteEvent: (ImageEvent) -> Unit,
    parentItem: StateFlow<Scenario?>,
) : EventsEditor<ImageEvent, ImageCondition>(onDeleteEvent, canBeEmpty = true, parentItem) {

    override fun onEditedEventConditionsUpdated(conditions: List<ImageCondition>) {
        val editedEvent = editedItem.value ?: return

        actionsEditor.editedList.value?.let { actions ->
            val newActions = actions.toMutableList()
            actions.forEach { action ->
                when {
                    // Skip all actions but clicks
                    action !is Click -> return@forEach

                    // Nothing to do on user selected position
                    action.positionType == Click.PositionType.USER_SELECTED -> return@forEach

                    // Condition was referenced and used by an action, delete it
                    editedEvent.conditionOperator == AND && conditions.find { action.clickOnConditionId == it.id } == null ->
                        newActions.remove(action)

                    // Condition was referenced but not used by an action, delete the reference
                    editedEvent.conditionOperator == OR && action.clickOnConditionId != null ->
                        newActions[newActions.indexOf(action)] = action.copy(clickOnConditionId = null)
                }
            }

            actionsEditor.updateList(newActions)
        }

        editedItem.value?.let { event ->
            updateEditedItem(copyEventWithNewChildren(event, conditions = conditions))
        }
    }

    override fun copyEventWithNewChildren(
        event: ImageEvent,
        conditions: List<ImageCondition>,
        actions: List<Action>,
    ): ImageEvent = event.copy(conditions = conditions, actions = actions)

}