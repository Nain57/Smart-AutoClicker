/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.references

import android.util.Log
import com.buzbuz.smartautoclicker.core.domain.model.action.ToggleEvent
import com.buzbuz.smartautoclicker.core.domain.model.action.toggleevent.EventToggle
import com.buzbuz.smartautoclicker.core.domain.model.event.Event
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.MissingCopyReference
import javax.inject.Inject

class ReplaceMissingEventToggleReferenceUseCase @Inject constructor() {

    operator fun invoke(
        event: Event,
        itemToEdit: ItemWithMissingReferences.ActionItem,
        missingReference: MissingCopyReference.EventToggleReference,
        replacement: List<EventToggle>,
    ): Event {

        val actionToEdit =
            if (itemToEdit.item is ToggleEvent) itemToEdit.item
            else {
                Log.e(TAG, "Can't replace, action is not a ToggleEvent action.")
                return event
            }

        val actionIndex = event.actions.indexOf(actionToEdit)
        if (actionIndex !in event.actions.indices) {
            Log.e(TAG, "Can't find action to edit in provided event.")
            return event
        }

        return event.copyBase(
            actions = event.actions.toMutableList().apply {
                set(actionIndex, actionToEdit.copy(eventToggles = replacement))
            }
        )
    }
}

private const val TAG = "ReplaceMissingEventToggleReferenceUseCase"