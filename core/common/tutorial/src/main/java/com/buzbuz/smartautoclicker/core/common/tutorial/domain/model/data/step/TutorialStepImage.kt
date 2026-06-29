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
package com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * An optional image shown inside a [TutorialStep.TutorialOverlay].
 *
 * @property imageResId drawable resource for the image itself.
 * @property imageDescResId string resource for the image's content description (accessibility).
 */
data class TutorialStepImage(
    @field:DrawableRes val imageResId: Int,
    @field:StringRes val imageDescResId: Int,
)