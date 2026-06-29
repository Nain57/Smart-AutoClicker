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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data

import androidx.annotation.StringRes

/**
 * Display metadata for a tutorial shown in selection UIs.
 *
 * @property id unique identifier for the tutorial.
 * @property nameResId string resource for the tutorial's short title.
 * @property descResId string resource for the tutorial's longer description.
 */
data class TutorialInfo(
    val id: String,
    @field:StringRes val nameResId: Int,
    @field:StringRes val descResId: Int,
)