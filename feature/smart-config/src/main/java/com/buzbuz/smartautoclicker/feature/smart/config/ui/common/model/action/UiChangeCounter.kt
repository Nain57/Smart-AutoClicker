
package com.buzbuz.smartautoclicker.feature.smart.config.ui.common.model.action

import android.content.Context
import androidx.annotation.DrawableRes
import com.buzbuz.smartautoclicker.core.domain.model.action.ChangeCounter
import com.buzbuz.smartautoclicker.feature.smart.config.R


@DrawableRes
internal fun getChangeCounterIconRes(): Int =
    R.drawable.ic_change_counter

internal fun ChangeCounter.getDescription(context: Context, inError: Boolean): String =
    if (inError) context.getString(R.string.item_change_counter_details_error)
    else context.getString(
        R.string.item_change_counter_details,
        counterName.trim(),
        when (operation) {
            ChangeCounter.OperationType.ADD -> "+"
            ChangeCounter.OperationType.MINUS -> "-"
            ChangeCounter.OperationType.SET -> "="
        },
        operationValue.value.toString(),
    )