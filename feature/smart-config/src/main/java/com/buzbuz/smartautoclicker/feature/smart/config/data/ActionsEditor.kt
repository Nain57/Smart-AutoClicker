
package com.buzbuz.smartautoclicker.feature.smart.config.data

import com.buzbuz.smartautoclicker.core.domain.model.AND
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.core.domain.model.action.Click
import com.buzbuz.smartautoclicker.core.domain.model.action.Intent
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.action.intent.IntentExtra
import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.data.base.ListEditor

import kotlinx.coroutines.flow.StateFlow

internal class ActionsEditor<Parent>(
    onListUpdated: (List<Action>) -> Unit,
    parentItem: StateFlow<Parent?>,
): ListEditor<Action, Parent>(onListUpdated, parentItem = parentItem) {

    val intentExtraEditor: ListEditor<IntentExtra<out Any>, Action> = ListEditor(
        onListUpdated = ::onEditedActionIntentExtraUpdated,
        canBeEmpty = true,
        parentItem = editedItem,
    )

    val eventToggleEditor: ListEditor<EventToggle, Action> = ListEditor(
        onListUpdated = ::onEditedActionEventToggleUpdated,
        canBeEmpty = true,
        parentItem = editedItem,
    )

    override fun startItemEdition(item: Action) {
        super.startItemEdition(item)

        when (item) {
            is Intent -> intentExtraEditor.startEdition(item.extras ?: emptyList())
            is ToggleEvent -> eventToggleEditor.startEdition(item.eventToggles)
            else -> Unit
        }
    }

    override fun stopItemEdition() {
        intentExtraEditor.stopEdition()
        super.stopItemEdition()
    }

    override fun itemCanBeSaved(item: Action?, parent: Parent?): Boolean =
        if (item is Click) {
            when (parent) {
                is TriggerEvent ->
                    item.isComplete() && item.positionType != Click.PositionType.ON_DETECTED_CONDITION

                is ImageEvent ->
                    if (item.isComplete()) !(parent.conditionOperator == AND && !item.isClickOnConditionValid())
                    else false

                else -> item.isComplete()
            }
        } else item?.isComplete() ?: false

    private fun onEditedActionIntentExtraUpdated(extras: List<IntentExtra<out Any>>) {
        val action = editedItem.value
        if (action == null || action !is Intent) return

        updateEditedItem(action.copy(extras = extras))
    }

    private fun onEditedActionEventToggleUpdated(eventToggles: List<EventToggle>) {
        val action = editedItem.value
        if (action == null || action !is ToggleEvent) return

        updateEditedItem(action.copy(eventToggles = eventToggles))
    }
}