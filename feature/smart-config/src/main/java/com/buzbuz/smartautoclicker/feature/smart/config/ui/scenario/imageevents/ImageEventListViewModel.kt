
package com.buzbuz.smartautoclicker.feature.smart.config.ui.scenario.imageevents

import android.content.Context
import android.view.View

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.domain.model.event.ImageEvent
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.smart.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.UiImageEvent
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event.toUiImageEvent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

import javax.inject.Inject

class ImageEventListViewModel @Inject constructor(
    private val editionRepository: EditionRepository,
    private val monitoredViewsManager: MonitoredViewsManager,
) : ViewModel() {

    /** Currently configured events. */
    val eventsItems = editionRepository.editionState.editedImageEventsState
        .mapNotNull { imageEventsState ->
            imageEventsState.value?.map { imageEvent ->
                imageEvent.toUiImageEvent(inError = !imageEvent.isComplete())
            }
        }

    /** Tells if the copy button should be visible or not. */
    val copyButtonIsVisible: Flow<Boolean> = editionRepository.editionState.canCopyImageEvents

    /**
     * Creates a new event item.
     * @param context the Android context.
     * @return the new event item.
     */
    fun createNewEvent(context: Context, event: ImageEvent? = null): ImageEvent = with(editionRepository.editedItemsBuilder) {
        if (event == null) createNewImageEvent(context)
        else createNewImageEventFrom(event)
    }

    fun startEventEdition(event: ImageEvent) = editionRepository.startEventEdition(event)

    /** Add or update an event. If the event id is unset, it will be added. If not, updated. */
    fun saveEventEdition() = editionRepository.upsertEditedEvent()

    /** Delete an event. */
    fun deleteEditedEvent() = editionRepository.deleteEditedEvent()

    /** Drop all changes made to the currently edited event. */
    fun dismissEditedEvent() = editionRepository.stopEventEdition()

    /** Update the priority of the events in the scenario. */
    fun updateEventsPriority(uiEvents: List<UiImageEvent>) =
        editionRepository.updateImageEventsOrder(
            uiEvents.map { it.event }
        )

    fun monitorFirstEventView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.SCENARIO_DIALOG_ITEM_FIRST_EVENT, view)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.SCENARIO_DIALOG_ITEM_FIRST_EVENT)
    }
}
