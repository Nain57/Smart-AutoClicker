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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.copy.fix

import android.content.Context

import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.domain.usecase.copy.model.ItemWithMissingReferences
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.action.toUiAction
import com.buzbuz.smartautoclicker.feature.smart.config.ui.common.uistate.condition.toUiCondition


internal fun ItemWithMissingReferences.ActionItem.toFixCopyUiItem(
    context: Context,
): FixCopyUiItem.Item.EventChildren.ActionItem {
    val isValid = missingReferences.isEmpty()
    return FixCopyUiItem.Item.EventChildren.ActionItem(
        uiAction = item.toUiAction(context, inError = false),
        stateText = context.getFixCopyStateText(isValid),
        isValidForCopy = isValid,
        itemWithMissingReferences = this,
    )
}

internal fun ItemWithMissingReferences.ConditionItem.toFixCopyUiItem(
    context: Context,
): FixCopyUiItem.Item.EventChildren.ConditionItem {
    val isValid = missingReferences.isEmpty()
    return FixCopyUiItem.Item.EventChildren.ConditionItem(
        uiCondition = item.toUiCondition(context, inError = false),
        stateText = context.getFixCopyStateText(isValid),
        isValidForCopy = isValid,
        itemWithMissingReferences = this,
    )
}

internal fun List<FixCopyUiItem>.toFixCopyUiState(): FixEventsChildrenCopyUiState =
    FixEventsChildrenCopyUiState(
        canBeCopied = filterIsInstance<FixCopyUiItem.Item.EventChildren>().all { it.isValidForCopy },
        items = this,
    )

private fun Context.getFixCopyStateText(isValid: Boolean): String =
    if (isValid) getString(R.string.item_copy_fix_description_ok)
    else getString(R.string.item_copy_fix_description_missing)
