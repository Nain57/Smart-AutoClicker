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
package com.buzbuz.smartautoclicker.core.dumb.domain.model

import com.buzbuz.smartautoclicker.core.base.identifier.Identifier
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioEntity
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions

internal fun DumbScenarioWithActions.toDomain(asDomain: Boolean = false): DumbScenario =
    DumbScenario(
        id = Identifier(id = scenario.id, asDomain = asDomain),
        name = scenario.name,
        repeatCount = scenario.repeatCount,
        isRepeatInfinite = scenario.isRepeatInfinite,
        maxDurationMin = scenario.maxDurationMin,
        isDurationInfinite = scenario.isDurationInfinite,
        randomize = scenario.randomize,
        dumbActions = dumbActions
            .sortedBy { it.priority }
            .map { dumbAction -> dumbAction.toDomain(asDomain) }
    )

internal fun DumbScenario.toEntity(): DumbScenarioEntity =
    DumbScenarioEntity(
        id = id.databaseId,
        name = name,
        repeatCount = repeatCount,
        isRepeatInfinite = isRepeatInfinite,
        maxDurationMin = maxDurationMin,
        isDurationInfinite = isDurationInfinite,
        randomize = randomize,
    )