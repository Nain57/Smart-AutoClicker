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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.action.click

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Point
import android.view.View

import androidx.lifecycle.AndroidViewModel

import com.buzbuz.smartautoclicker.core.ui.bindings.DropdownItem
import com.buzbuz.smartautoclicker.core.domain.model.action.Action
import com.buzbuz.smartautoclicker.feature.scenario.config.R
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.feature.scenario.config.utils.putClickPressDurationConfig
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.ui.monitoring.ViewPositioningType

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull

class ClickViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the edited items. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    /** The action being configured by the user. */
    private val configuredClick = editionRepository.editionState.editedActionState
        .mapNotNull { action -> action.value }
        .filterIsInstance<Action.Click>()
    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = application.getEventConfigPreferences()

    /** The name of the click. */
    val name: Flow<String?> = configuredClick
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredClick.map { it.name?.isEmpty() ?: true }

    /** The duration between the press and release of the click in milliseconds. */
    val pressDuration: Flow<String?> = configuredClick
        .map { it.pressDuration?.toString() }
        .take(1)
    /** Tells if the press duration value is valid or not. */
    val pressDurationError: Flow<Boolean> = configuredClick.map { (it.pressDuration ?: -1) <= 0 }

    /** The position of the click. */
    val position: Flow<Point?> = configuredClick
        .map { click ->
            if (click.clickOnCondition || click.x == null || click.y == null) null
            else Point(click.x!!, click.y!!)
        }

    val clickTypeItemOnCondition = DropdownItem(
        title = R.string.dropdown_item_title_click_position_type_on_condition,
        helperText = R.string.dropdown_helper_text_click_position_type_on_condition,
    )
    val clickTypeItemOnPosition = DropdownItem(
        title= R.string.dropdown_item_title_click_position_type_on_position,
        helperText = R.string.dropdown_helper_text_click_position_type_on_position,
    )
    /** Items for the click type dropdown field. */
    val clickTypeItems = listOf(clickTypeItemOnCondition, clickTypeItemOnPosition)
    /** If the click should be made on the detected condition. */
    val clickOnCondition: Flow<DropdownItem> = configuredClick
        .map { click ->
            when (click.clickOnCondition) {
                true -> clickTypeItemOnCondition
                false -> clickTypeItemOnPosition
            }
        }
        .filterNotNull()

    /** Tells if the configured click is valid and can be saved. */
    val isValidAction: Flow<Boolean> = editionRepository.editionState.editedActionState
        .map { it.canBeSaved }

    /**
     * Set the name of the click.
     * @param name the new name.
     */
    fun setName(name: String) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(name = "" + name))
        }
    }

    /** Set if this click should be made on the detected condition. */
    fun setClickOnCondition(newItem: DropdownItem) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            val clickOnCondition = when (newItem) {
                clickTypeItemOnCondition -> true
                clickTypeItemOnPosition -> false
                else -> return
            }

            editionRepository.updateEditedAction(click.copy(clickOnCondition = clickOnCondition))
        }
    }

    /**
     * Set the position of the click.
     * @param position the new position.
     */
    fun setPosition(position: Point) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(x = position.x, y = position.y))
        }
    }

    /**
     * Set the press duration of the click.
     * @param durationMs the new duration in milliseconds.
     */
    fun setPressDuration(durationMs: Long?) {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            editionRepository.updateEditedAction(click.copy(pressDuration = durationMs))
        }
    }

    /** Save the configured values to restore them at next creation. */
    fun saveLastConfig() {
        editionRepository.editionState.getEditedAction<Action.Click>()?.let { click ->
            sharedPreferences.edit().putClickPressDurationConfig(click.pressDuration ?: 0).apply()
        }
    }

    fun monitorSaveButtonView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE, view)
    }

    fun monitorSelectPositionView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CLICK_DIALOG_BUTTON_SELECT_POSITION, view)
    }

    fun monitorClickOnDropdownView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.CLICK_DIALOG_DROPDOWN_CLICK_ON, view)
    }

    fun monitorDropdownItemConditionView(view: View) {
        monitoredViewsManager.attach(
            MonitoredViewType.CLICK_DIALOG_DROPDOWN_ITEM_CLICK_ON_CONDITION,
            view,
            ViewPositioningType.SCREEN,
        )
    }

    fun stopDropdownItemConditionViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.CLICK_DIALOG_DROPDOWN_ITEM_CLICK_ON_CONDITION)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.CLICK_DIALOG_BUTTON_SAVE)
        monitoredViewsManager.detach(MonitoredViewType.CLICK_DIALOG_BUTTON_SELECT_POSITION)
        monitoredViewsManager.detach(MonitoredViewType.CLICK_DIALOG_DROPDOWN_ITEM_CLICK_ON_CONDITION)
        monitoredViewsManager.detach(MonitoredViewType.CLICK_DIALOG_DROPDOWN_CLICK_ON)
    }
}