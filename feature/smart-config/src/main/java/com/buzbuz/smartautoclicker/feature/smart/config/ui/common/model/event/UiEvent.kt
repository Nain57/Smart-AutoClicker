
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.event

import com.buzbuz.smartautoclicker.core.domain.model.event.Event

sealed class UiEvent {
    abstract val event: Event
}