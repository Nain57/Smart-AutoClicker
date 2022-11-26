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
package com.buzbuz.smartautoclicker.overlays.config.event.conditions

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.domain.*
import com.buzbuz.smartautoclicker.overlays.base.utils.newDefaultCondition

import kotlinx.coroutines.*

import kotlinx.coroutines.flow.*

class ConditionsViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application.applicationContext)

    /** Tells if there is at least one condition to copy. */
    val canCopyCondition: Flow<Boolean> = repository.getAllConditions()
        .map { it.isNotEmpty() }

    /** The event currently configured. */
    private lateinit var configuredEvent: MutableStateFlow<Event?>

    /** Backing property for [conditions]. */
    private val _conditions by lazy {
        configuredEvent
            .map { it?.conditions }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(),
                emptyList()
            )
    }
    /** The event conditions currently edited by the user. */
    val conditions: StateFlow<List<Condition>?> get() = _conditions

    /** Set the event currently configured by the UI. */
    fun setConfiguredEvent(event: MutableStateFlow<Event?>) {
        configuredEvent = event
    }

    /**
     * Create a new condition with the default values from configuration.
     *
     * @param context the Android Context.
     * @param area the area of the condition to create.
     * @param bitmap the image for the condition to create.
     */
    fun createCondition(context: Context, area: Rect, bitmap: Bitmap): Condition {
        configuredEvent.value?.let { event ->
            return newDefaultCondition(
                context = context,
                eventId = event.id,
                bitmap = bitmap,
                area = area,
            )
        } ?: throw IllegalStateException("Can't create a condition, event is null!")
    }

    /**
     * Add a new condition to the event.
     * @param condition the new condition.
     */
    fun addCondition(condition: Condition) {
        configuredEvent.value?.let { event ->
            val newConditions = event.conditions?.let { ArrayList(it) } ?: ArrayList()
            newConditions.add(condition)

            viewModelScope.launch {
                configuredEvent.value = event.copy(conditions = newConditions)
            }
        }
    }

    /**
     * Update a condition in the event.
     * @param condition the updated condition.
     */
    fun updateCondition(condition: Condition, index: Int) {
        configuredEvent.value?.let { event ->
            val newConditions = event.conditions?.let { ArrayList(it) } ?: ArrayList()
            newConditions[index] = condition

            viewModelScope.launch {
                configuredEvent.value = event.copy(conditions = newConditions)
            }
        }
    }

    /**
     * Remove a condition from the event.
     * @param condition the condition to be removed.
     */
    fun removeCondition(condition: Condition) {
        configuredEvent.value?.let { event ->

            val newConditions = event.conditions?.let { ArrayList(it) } ?: ArrayList()
            if (newConditions.remove(condition)) {
                viewModelScope.launch {
                    configuredEvent.value = event.copy(conditions = newConditions)
                }
            }
        }
    }

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: Condition, onBitmapLoaded: (Bitmap?) -> Unit): Job? {
        if (condition.bitmap != null) {
            onBitmapLoaded.invoke(condition.bitmap)
            return null
        }

        if (condition.path != null) {
            return viewModelScope.launch(Dispatchers.IO) {
                val bitmap = repository.getBitmap(condition.path!!, condition.area.width(), condition.area.height())

                if (isActive) {
                    withContext(Dispatchers.Main) {
                        onBitmapLoaded.invoke(bitmap)
                    }
                }
            }
        }

        onBitmapLoaded.invoke(null)
        return null
    }
}
