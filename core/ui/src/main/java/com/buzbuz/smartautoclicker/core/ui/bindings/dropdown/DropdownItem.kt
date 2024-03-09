package com.buzbuz.smartautoclicker.core.ui.bindings.dropdown

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

import com.buzbuz.smartautoclicker.core.ui.R

open class DropdownItem(
    @StringRes val title: Int,
    @StringRes val helperText: Int? = null,
    @DrawableRes val icon: Int? = null,
)

sealed class TimeUnitDropDownItem(@StringRes title: Int) : DropdownItem(title) {
    data object Milliseconds : TimeUnitDropDownItem(R.string.dropdown_label_time_unit_ms)
    data object Seconds : TimeUnitDropDownItem(R.string.dropdown_label_time_unit_s)
    data object Minutes : TimeUnitDropDownItem(R.string.dropdown_label_time_unit_min)
    data object Hours : TimeUnitDropDownItem(R.string.dropdown_label_time_unit_h)
}

val timeUnitDropdownItems: List<TimeUnitDropDownItem>
    get() = listOf(
        TimeUnitDropDownItem.Milliseconds,
        TimeUnitDropDownItem.Seconds,
        TimeUnitDropDownItem.Minutes,
        TimeUnitDropDownItem.Hours,
    )

fun Long?.toDurationMs(unit: TimeUnitDropDownItem): Long =
    when {
        this == null -> -1
        unit == TimeUnitDropDownItem.Seconds -> this * 1.seconds.inWholeMilliseconds
        unit == TimeUnitDropDownItem.Minutes -> this * 1.minutes.inWholeMilliseconds
        unit == TimeUnitDropDownItem.Hours -> this * 1.hours.inWholeMilliseconds
        else -> this
    }

fun Long?.findAppropriateTimeUnit(): TimeUnitDropDownItem =
    when {
        this == null || this <= 0L -> TimeUnitDropDownItem.Milliseconds
        this % 1.hours.inWholeMilliseconds == 0L -> TimeUnitDropDownItem.Hours
        this % 1.minutes.inWholeMilliseconds == 0L -> TimeUnitDropDownItem.Minutes
        this % 1.seconds.inWholeMilliseconds == 0L -> TimeUnitDropDownItem.Seconds
        else -> TimeUnitDropDownItem.Milliseconds
    }

fun TimeUnitDropDownItem.formatDuration(durationMs: Long): String =
    when (this) {
        TimeUnitDropDownItem.Seconds -> durationMs / 1.seconds.inWholeMilliseconds
        TimeUnitDropDownItem.Minutes -> durationMs / 1.minutes.inWholeMilliseconds
        TimeUnitDropDownItem.Hours -> durationMs / 1.hours.inWholeMilliseconds
        else -> durationMs
    }.toString()