/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.overlays.config.action.click

import android.app.Application
import android.content.SharedPreferences
import android.graphics.Point

import androidx.lifecycle.AndroidViewModel
import com.buzbuz.smartautoclicker.R

import com.buzbuz.smartautoclicker.domain.Action
import com.buzbuz.smartautoclicker.overlays.base.bindings.DropdownItem
import com.buzbuz.smartautoclicker.overlays.base.utils.getEventConfigPreferences
import com.buzbuz.smartautoclicker.overlays.base.utils.isValidDuration
import com.buzbuz.smartautoclicker.overlays.base.utils.putClickPressDurationConfig

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class ClickViewModel(application: Application) : AndroidViewModel(application) {

    /** The action being configured by the user. Defined using [setConfiguredClick]. */
    private val configuredClick = MutableStateFlow<Action.Click?>(null)
    /** Event configuration shared preferences. */
    private val sharedPreferences: SharedPreferences = application.getEventConfigPreferences()

    /** The name of the click. */
    val name: Flow<String?> = configuredClick
        .filterNotNull()
        .map { it.name }
        .take(1)
    /** Tells if the action name is valid or not. */
    val nameError: Flow<Boolean> = configuredClick.map { it?.name?.isEmpty() ?: true }

    /** The duration between the press and release of the click in milliseconds. */
    val pressDuration: Flow<String?> = configuredClick
        .filterNotNull()
        .map { it.pressDuration?.toString() }
        .take(1)
    /** Tells if the press duration value is valid or not. */
    val pressDurationError: Flow<Boolean> = configuredClick.map { (it?.pressDuration ?: -1) <= 0 }

    /** The position of the click. */
    val position: Flow<Point?> = configuredClick
        .filterNotNull()
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
            when (click?.clickOnCondition) {
                true -> clickTypeItemOnCondition
                false -> clickTypeItemOnPosition
                null -> null
            }
        }
        .filterNotNull()

    /** Tells if the configured click is valid and can be saved. */
    val isValidAction: Flow<Boolean> = configuredClick
        .map { click ->
            click != null && !click.name.isNullOrEmpty()
                    && ((click.x != null && click.y != null) || click.clickOnCondition)
                    && click.pressDuration.isValidDuration()
        }

    /**
     * Set the configured click.
     * This will update all values represented by this view model.
     *
     * @param click the click to configure.
     */
    fun setConfiguredClick(click: Action.Click) {
        configuredClick.value = click.deepCopy()
    }

    /** @return the click containing all user changes. */
    fun getConfiguredClick(): Action.Click =
        configuredClick.value ?: throw IllegalStateException("Can't get the configured click, none were defined.")

    /**
     * Set the name of the click.
     * @param name the new name.
     */
    fun setName(name: String) {
        configuredClick.value?.let { click ->
            configuredClick.value = click.copy(name = "" + name)
        }
    }

    /** Set if this click should be made on the detected condition. */
    fun setClickOnCondition(newItem: DropdownItem) {
        configuredClick.value?.let { click ->
            val clickOnCondition = when (newItem) {
                clickTypeItemOnCondition -> true
                clickTypeItemOnPosition -> false
                else -> return
            }
            configuredClick.value = click.copy(clickOnCondition = clickOnCondition)
        }
    }

    /**
     * Set the position of the click.
     * @param position the new position.
     */
    fun setPosition(position: Point) {
        configuredClick.value?.let { click ->
            configuredClick.value =  click.copy(x = position.x, y = position.y)
        }
    }

    /**
     * Set the press duration of the click.
     * @param durationMs the new duration in milliseconds.
     */
    fun setPressDuration(durationMs: Long?) {
        configuredClick.value?.let { click ->
            configuredClick.value = click.copy(pressDuration = durationMs)
        }
    }

    /** Save the configured values to restore them at next creation. */
    fun saveLastConfig() {
        configuredClick.value?.let { click ->
            sharedPreferences.edit().putClickPressDurationConfig(click.pressDuration ?: 0).apply()
        }
    }
}