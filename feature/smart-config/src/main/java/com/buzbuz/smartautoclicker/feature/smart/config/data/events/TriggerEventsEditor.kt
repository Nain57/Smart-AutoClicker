
package com.buzbuz.smartautoclicker.feature.smart.config.data.events

import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.condition.TriggerCondition
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.core.domain.model.scenario.Scenario

import kotlinx.coroutines.flow.StateFlow

internal class TriggerEventsEditor(
    onDeleteEvent: (TriggerEvent) -> Unit,
    parentItem: StateFlow<Scenario?>,
) : EventsEditor<TriggerEvent, TriggerCondition>(onDeleteEvent, canBeEmpty = true, parentItem) {

    override fun onEditedEventConditionsUpdated(conditions: List<TriggerCondition>) {
        editedItem.value?.let { event ->
            updateEditedItem(copyEventWithNewChildren(event, conditions = conditions))
        }
    }

    override fun copyEventWithNewChildren(
        event: TriggerEvent,
        conditions: List<TriggerCondition>,
        actions: List<Action>
    ): TriggerEvent = event.copy(conditions = conditions, actions = actions)
}