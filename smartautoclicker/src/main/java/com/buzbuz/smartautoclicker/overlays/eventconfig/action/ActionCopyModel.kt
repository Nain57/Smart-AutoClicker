/*
 * Copyright (C) 2021 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.overlays.eventconfig.action

import android.content.Context

import com.buzbuz.smartautoclicker.baseui.OverlayViewModel
import com.buzbuz.smartautoclicker.database.Repository
import com.buzbuz.smartautoclicker.database.domain.Action

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * View model for the [ActionCopyDialog].
 *
 * @param context the Android context.
 */
class ActionCopyModel(context: Context) : OverlayViewModel(context) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(context)

    /** The list of actions for the configured event. They are not all available yet in the database. */
    private val eventActions = MutableStateFlow<List<Action>?>(null)
    /** List of all actions. */
    val actionList: Flow<List<Action>?> = repository.getAllActions()
        .combine(eventActions) { dbActions, eventActions ->
            if (eventActions == null) return@combine null

            val allActions = mutableListOf<Action>()
            allActions.addAll(eventActions)
            allActions.sortBy { it.getActionName() }
            val otherEventActions = dbActions.toMutableList().apply {
                removeIf { allAction ->
                    eventActions.find { allAction.getIdentifier() == it.getIdentifier() } != null
                }
            }
            allActions.addAll(otherEventActions)

            allActions
        }

    /**
     * Set the current event actions.
     * @param actions the actions.
     */
    fun setCurrentEventActions(actions: List<Action>) {
        eventActions.value = actions
    }

    /**
     * Get a new action based on the provided one.
     * @param action the acton to copy.
     */
    fun getNewActionForCopy(action: Action): Action =
        when(action) {
            is Action.Click -> action.copy(id = 0, name = "" + action.getActionName())
            is Action.Swipe -> action.copy(id = 0, name = "" + action.getActionName())
            is Action.Pause -> action.copy(id = 0, name = "" + action.getActionName())
        }
}