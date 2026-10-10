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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.selection

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.core.bitmaps.BitmapRepository
import com.buzbuz.smartautoclicker.core.domain.model.condition.ScreenCondition
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.monitoring.MonitoredViewType
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.MonitoredViewsManager
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.condition.UiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.condition.toUiScreenCondition
import com.buzbuz.smartautoclicker.feature.smart.config.utils.getImageConditionBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import kotlin.collections.map


class ScreenConditionSelectionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val bitmapRepository: BitmapRepository,
    private val monitoredViewsManager: MonitoredViewsManager
) : ViewModel() {

    private val conditions: MutableStateFlow<List<ScreenCondition>?> = MutableStateFlow(null)
    val uiConditions: StateFlow<List<UiScreenCondition>?> = conditions
        .map { modelConditions -> modelConditions?.toUiScreenConditions(context) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(3_000), null)


    fun setSelectableConditions(selectableConditions: List<ScreenCondition>) {
        conditions.update { selectableConditions }
    }

    /**
     * Get the bitmap corresponding to a condition.
     * Loading is async and the result notified via the onBitmapLoaded argument.
     *
     * @param condition the condition to load the bitmap of.
     * @param onBitmapLoaded the callback notified upon completion.
     */
    fun getConditionBitmap(condition: ScreenCondition.Image, onBitmapLoaded: (Bitmap?) -> Unit): Job =
        getImageConditionBitmap(bitmapRepository, condition, onBitmapLoaded)

    fun monitorFirstConditionItemView(itemView: View) {
        monitoredViewsManager.attach(MonitoredViewType.CONDITION_SELECTOR_DIALOG_ITEM_FIRST, itemView)
    }

    fun stopViewMonitoring() {
        monitoredViewsManager.detach(MonitoredViewType.CONDITION_SELECTOR_DIALOG_ITEM_FIRST)
    }

    private fun List<ScreenCondition>.toUiScreenConditions(context: Context): List<UiScreenCondition> =
        map { modelCondition ->
            modelCondition.toUiScreenCondition(
                context = context,
                shortThreshold = true,
                inError = !modelCondition.isComplete(),
            )
        }
}