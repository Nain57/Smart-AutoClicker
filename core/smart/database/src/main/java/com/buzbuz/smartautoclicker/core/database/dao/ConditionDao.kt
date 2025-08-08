
package com.buzbuz.smartautoclicker.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.buzbuz.smartautoclicker.core.database.CONDITION_TABLE

import com.buzbuz.smartautoclicker.core.database.entity.ConditionEntity
import com.buzbuz.smartautoclicker.core.database.entity.ConditionType

import kotlinx.coroutines.flow.Flow

/** Allows to access the conditions in the database. */
@Dao
abstract class ConditionDao {

    /**
     * Get all conditions from all events.
     *
     * @return the list containing all conditions.
     */
    @Query("SELECT * FROM $CONDITION_TABLE")
    abstract fun getAllConditions(): Flow<List<ConditionEntity>>

    /**
     * Get the list of conditions for a given event.
     *
     * @param eventId the identifier of the event to get the conditions from.
     * @return the list of conditions for the event.
     */
    @Query("SELECT * FROM $CONDITION_TABLE WHERE eventId=:eventId ORDER BY priority")
    abstract suspend fun getConditions(eventId: Long): List<ConditionEntity>

    /**
     * Get the list of image conditions that uses the legacy image format
     * @return the list of legacy conditions.
     */
    @Query("SELECT * FROM $CONDITION_TABLE WHERE type='ON_IMAGE_DETECTED' AND path IS NOT NULL AND path NOT LIKE '%.png'")
    abstract fun getLegacyImageConditionsFlow(): Flow<List<ConditionEntity>>

    /**
     * Get the list of image conditions that uses the legacy image format
     * @return the list of legacy conditions.
     */
    @Query("SELECT * FROM $CONDITION_TABLE WHERE type='ON_IMAGE_DETECTED' AND path IS NOT NULL AND path NOT LIKE '%.png'")
    abstract suspend fun getLegacyImageConditions(): List<ConditionEntity>

    /**
     * Get the list of conditions path for a given event.
     *
     * @param eventId the identifier of the event to get the conditions path from.
     * @return the list of path for the event.
     */
    @Query("SELECT path FROM $CONDITION_TABLE WHERE eventId=:eventId AND type='ON_IMAGE_DETECTED'")
    abstract suspend fun getConditionsPaths(eventId: Long): List<String>

    /**
     * Get the number of times this path is used in the condition table.
     *
     * @param path the value to be searched in the path column.
     * @return the number of conditions using this path.
     */
    @Query("SELECT COUNT(path) FROM $CONDITION_TABLE WHERE path=:path AND type='ON_IMAGE_DETECTED'")
    abstract suspend fun getValidPathCount(path: String): Int

    /**
     * Add conditions to the database.
     * @param conditions the conditions to be added.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addConditions(conditions: List<ConditionEntity>): List<Long>

    /**
     * Update a condition in the database.
     * @param condition the condition to be updated.
     */
    @Update
    abstract suspend fun updateCondition(condition: ConditionEntity)

    /**
     * Update a condition in the database.
     * @param conditions the condition to be updated.
     */
    @Update
    abstract suspend fun updateConditions(conditions: List<ConditionEntity>)

    /**
     * Delete a list of conditions in the database.
     * @param conditions the conditions to be removed.
     */
    @Delete
    abstract suspend fun deleteConditions(conditions: List<ConditionEntity>)
}