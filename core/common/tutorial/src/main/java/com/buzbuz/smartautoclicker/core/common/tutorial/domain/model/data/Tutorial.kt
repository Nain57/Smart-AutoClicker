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

import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.step.TutorialStep
import com.buzbuz.smartautoclicker.core.common.tutorial.domain.model.data.subject.TutorialSubject

/**
 * Root definition of a single tutorial.
 *
 * Implementations are declared in the feature module and passed to the engine at runtime.
 * The engine drives the [steps] sequence and delegates subject-specific logic to a controller
 * created from [subject].
 *
 * @property info Display metadata (name and description string resources).
 * @property subject The interactive subject (e.g. a mini-game) that runs alongside the step sequence.
 * @property steps Ordered sequence of steps the user must complete to finish the tutorial.
 */
data class Tutorial(
    val info: TutorialInfo,
    val subject: TutorialSubject,
    val steps: List<TutorialStep>,
)

