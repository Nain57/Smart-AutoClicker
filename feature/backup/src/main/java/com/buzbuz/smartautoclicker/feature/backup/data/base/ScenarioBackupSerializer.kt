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
package com.buzbuz.smartautoclicker.feature.backup.data.base

import java.io.InputStream
import java.io.OutputStream

internal interface ScenarioBackupSerializer<T> {

    /**
     * Serialize a scenario.
     *
     * @param scenarioBackup the scenario backup to serialize.
     * @param outputStream the stream to serialize into.
     */
    fun serialize(scenarioBackup: T, outputStream: OutputStream)

    /**
     * Deserialize a scenario.
     * Depending of the detected version, either kotlin or compat serialization will be used.
     *
     * @param json the stream to deserialize from.
     *
     * @return the scenario backup deserialized from the json.
     */
    fun deserialize(json: InputStream): T?
}