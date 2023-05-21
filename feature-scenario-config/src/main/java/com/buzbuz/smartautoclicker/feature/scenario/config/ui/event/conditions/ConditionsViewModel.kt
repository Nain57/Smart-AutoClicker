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
package com.buzbuz.smartautoclicker.feature.scenario.config.ui.event.conditions

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.domain.Repository

import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository

import kotlinx.coroutines.*

import kotlinx.coroutines.flow.*

class ConditionsViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application.applicationContext)
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository = IBillingRepository.getRepository(application)

    /** Currently configured event. */
    private val configuredEvent = editionRepository.editedEvent
        .filterNotNull()

    /** Tells if the limitation in conditions count have been reached. */
    val isConditionLimitReached: Flow<Boolean> = billingRepository.isProModePurchased
        .combine(configuredEvent) { isProModePurchased, event ->
            !isProModePurchased && (event.conditions.size  >= ProModeAdvantage.Limitation.CONDITION_COUNT_LIMIT.limit)
        }

    /** Tells if there is at least one condition to copy. */
    val canCopyCondition: Flow<Boolean> = repository.getAllConditions()
        .map { it.isNotEmpty() }

    /** Tells if the pro mode billing flow is being displayed. */
    val isBillingFlowDisplayed: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    /** Backing property for [conditions]. */
    private val _conditions = configuredEvent
        .map { it.conditions }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            emptyList()
        )

    /** The event conditions currently edited by the user. */
    val conditions: StateFlow<List<Condition>?> get() = _conditions

    /**
     * Create a new condition with the default values from configuration.
     *
     * @param context the Android Context.
     * @param area the area of the condition to create.
     * @param bitmap the image for the condition to create.
     */
    fun createCondition(context: Context, area: Rect, bitmap: Bitmap): Condition =
        editionRepository.createNewCondition(context, area, bitmap, from = null)

    /**
     * Insert/update a new condition to the event.
     * @param condition the new condition.
     */
    fun upsertCondition(condition: Condition) =
        editionRepository.upsertConditionToEditedEvent(condition)

    /**
     * Remove a condition from the event.
     * @param condition the condition to be removed.
     */
    fun removeCondition(condition: Condition) =
        editionRepository.deleteConditionFromEditedEvent(condition)

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

    fun onConditionCountReachedAddCopyClicked(context: Context) {
        billingRepository.startBillingActivity(context, ProModeAdvantage.Limitation.CONDITION_COUNT_LIMIT)
    }
}
