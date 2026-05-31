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
package com.buzbuz.smartautoclicker.core.database.serialization.compat

import com.buzbuz.smartautoclicker.core.base.extensions.getInt
import kotlinx.serialization.json.JsonObject


/** Deserializer for all Json object version below 20. */
internal open class CompatV20Deserializer : CompatDeserializer() {

    override fun deserializeCounterConditionValue(jsonCounterCondition: JsonObject): Double =
        jsonCounterCondition.getInt("counterValue")?.toDouble() ?: 0.0

    override fun deserializeCounterActionValue(jsonCounterCondition: JsonObject): Double =
        jsonCounterCondition.getInt("counterOperationValue")?.toDouble() ?: 0.0
}