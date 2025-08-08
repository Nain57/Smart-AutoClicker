
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.triggerevents

import android.content.Context
import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.event.TriggerEvent
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.toUiTriggerEvent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject


class TriggerEventListViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
) : ViewModel() {

    /** Currently configured events. */
    val triggerEvents = editionRepository.editionState.editedTriggerEventsState
        .mapNotNull { triggerEventsState ->
            triggerEventsState.value?.map { triggerEvent ->
                triggerEvent.toUiTriggerEvent(inError = !triggerEvent.isComplete())
            }
        }

    /** Tells if the copy button should be visible or not. */
    val copyButtonIsVisible: Flow<Boolean> = editionRepository.editionState.canCopyTriggerEvents

    /**
     * Creates a new event item.
     * @param context the Android context.
     * @return the new event item.
     */
    fun createNewEvent(context: Context, event: TriggerEvent? = null): TriggerEvent = with(editionRepository.editedItemsBuilder) {
        if (event == null) createNewTriggerEvent(context)
        else createNewTriggerEventFrom(from = event)
    }

    fun startEventEdition(event: TriggerEvent) = editionRepository.startEventEdition(event)

    /** Add or update an event. If the event id is unset, it will be added. If not, updated. */
    fun saveEventEdition() = editionRepository.upsertEditedEvent()

    /** Delete an event. */
    fun deleteEditedEvent() = editionRepository.deleteEditedEvent()

    /** Drop all changes made to the currently edited event. */
    fun dismissEditedEvent() = editionRepository.stopEventEdition()
}
