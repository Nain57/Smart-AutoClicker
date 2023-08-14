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
import android.view.View

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.domain.Repository
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.ProModeAdvantage
import com.buzbuz.smartautoclicker.core.domain.model.condition.Condition
import com.buzbuz.smartautoclicker.feature.scenario.config.domain.EditionRepository
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewsManager
import com.buzbuz.smartautoclicker.core.ui.monitoring.MonitoredViewType

import kotlinx.coroutines.*

import kotlinx.coroutines.flow.*

class ConditionsViewModel(application: Application) : AndroidViewModel(application) {

    /** Repository providing access to the click database. */
    private val repository = Repository.getRepository(application.applicationContext)
    /** Maintains the currently configured scenario state. */
    private val editionRepository = EditionRepository.getInstance(application)
    /** The repository for the pro mode billing. */
    private val billingRepository = IBillingRepository.getRepository(application)
    /** Monitors views for the tutorial. */
    private val monitoredViewsManager: MonitoredViewsManager = MonitoredViewsManager.getInstance()

    /** Currently configured event. */
    val configuredEventConditions = editionRepository.editionState.editedEventConditionsState
        .mapNotNull { it.value }

    /** Tells if the limitation in conditions count have been reached. */
    val isConditionLimitReached: Flow<Boolean> = billingRepository.isProModePurchased
        .combine(configuredEventConditions) { isProModePurchased, conditions ->
            !isProModePurchased && (conditions.size  >= ProModeAdvantage.Limitation.CONDITION_COUNT_LIMIT.limit)
        }

    /** Tells if there is at least one condition to copy. */
    val canCopyCondition: Flow<Boolean> = combine(
        repository.getAllConditions(),
        configuredEventConditions,
        editionRepository.editionState.eventsState
    ) { dbConds, editedConds, scenarioEvents ->

        if (dbConds.isNotEmpty()) return@combine true
        if (editedConds.isNotEmpty()) return@combine true

        scenarioEvents.value?.forEach { event ->
            if (event.conditions.isNotEmpty()) return@combine true
        }
        false
    }

    /** Tells if the pro mode billing flow is being displayed. */
    val isBillingFlowDisplayed: Flow<Boolean> = billingRepository.isBillingFlowInProcess

    /**
     * Create a new condition with the default values from configuration.
     *
     * @param context the Android Context.
     * @param area the area of the condition to create.
     * @param bitmap the image for the condition to create.
     */
    fun createCondition(context: Context, area: Rect, bitmap: Bitmap): Condition =
        editionRepository.editedItemsBuilder.createNewCondition(context, area, bitmap)

    /**
     * Get a new condition based on the provided one.
     * @param condition the condition to copy.
     */
    fun createNewConditionFromCopy(condition: Condition): Condition =
        editionRepository.editedItemsBuilder.createNewConditionFrom(condition)

    fun startConditionEdition(condition: Condition) = editionRepository.startConditionEdition(condition)

    /** Insert/update a new condition to the event. */
    fun upsertEditedCondition() =
        editionRepository.upsertEditedCondition()

    /** Remove a condition from the event. */
    fun removeEditedCondition() =
        editionRepository.deleteEditedCondition()

    /** Drop all changes made to the currently edited event. */
    fun dismissEditedCondition() = editionRepository.stopConditionEdition()

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

    fun monitorCreateConditionView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_CONDITION, view)
    }

    fun monitorFirstConditionView(view: View) {
        monitoredViewsManager.attach(MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_CONDITION, view)
    }

    fun stopFirstConditionViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_CONDITION)

    }

    fun stopAllViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_BUTTON_CREATE_CONDITION)
        monitoredViewsManager.detach(MonitoredViewType.EVENT_DIALOG_ITEM_FIRST_CONDITION)
    }
}
