
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