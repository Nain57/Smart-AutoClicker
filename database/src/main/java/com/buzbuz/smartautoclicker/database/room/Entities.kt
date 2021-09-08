/*
 * Copyright (C) 2020 Nain57
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; If not, see <http://www.gnu.org/licenses/>.
 */
package com.buzbuz.smartautoclicker.database.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

// This file defines the structures of the ClickDatabase
// It declares three tables:
//  - The scenario_table, containing all click scenarios
//  - The click_table, containing all clicks, each one referencing its scenario
//  - The condition_table, containing all conditions information for the clicks
// A fourth table, identified by ClickConditionCrossRef, is a cross reference table between the click_table and the
// condition_table.

/**
 * Entity defining a scenario of clicks.
 *
 * A scenario has a relation "one to many" with [ClickEntity], which is represented by [ScenarioWithClicks].
 *
 * @param id the unique identifier for a scenario.
 * @param name the name of the scenario.
 */
@Entity(tableName = "scenario_table")
internal data class ScenarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "name") val name: String
)

/**
 * Entity defining a click.
 *
 * A click is a combination of an action (a single click or a gesture at a specific position) and a list of conditions
 * to fulfill in order to execute that action.
 *
 * It is always associated to a [ScenarioEntity] via its foreign key [ClickEntity.scenarioId]. Deletion of a
 * scenario will lead to the automatic deletion of all clicks with the same scenarioId.
 * It can be associated to several conditions via the cross reference object [ClickConditionCrossRef].
 *
 * @param clickId unique identifier for a click. Also the primary key in the table.
 * @param scenarioId identifier of this click's scenario. Reference the key [ScenarioEntity.id] in scenario_table.
 * @param name name of this click.
 * @param type type of this click. Can be any value of [com.buzbuz.smartautoclicker.database.ClickInfo.ClickType].
 * @param fromX the action x coordinates, or the action start x coordinates for gestures.
 * @param fromY the action y coordinates, or the action start y coordinates for gestures.
 * @param toX the action end x coordinates for gestures. Null if not a gesture.
 * @param toY the action end y coordinates for gestures. Null if not a gesture.
 * @param conditionOperator the operator to apply to all [ConditionEntity] related to this click. Can be any value
 *                          of [com.buzbuz.smartautoclicker.clicks.ClickInfo.Operator].
 * @param delayAfter the minimum delay between this click and the next one.
 * @param priority the order in the scenario. Lowest orders will always be checked first when detecting.
 */
@Entity(
    tableName = "click_table",
    indices = [Index("scenario_id")],
    foreignKeys = [ForeignKey(
        entity = ScenarioEntity::class,
        parentColumns = ["id"],
        childColumns = ["scenario_id"],
        onDelete = CASCADE
    )])
internal data class ClickEntity(
    @PrimaryKey(autoGenerate = true) val clickId: Long,
    @ColumnInfo(name = "scenario_id") val scenarioId: Long,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "type") var type: Int,
    @ColumnInfo(name = "from_x") val fromX: Int,
    @ColumnInfo(name = "from_y") val fromY: Int,
    @ColumnInfo(name = "to_x") val toX: Int,
    @ColumnInfo(name = "to_y") val toY: Int,
    @ColumnInfo(name = "operator") val conditionOperator: Int,
    @ColumnInfo(name = "delay_after") var delayAfter: Long,
    @ColumnInfo(name = "stop_after") var stopAfter: Int?,
    @ColumnInfo(name = "priority") var priority: Int
)

/**
 * Entity embedding a scenario and its clicks.
 *
 * Automatically do the junction between click_table and scenario using to provide this representation of the one to
 * many relation between scenario and clicks entity.
 *
 * @param scenario the scenario entity.
 * @param clicks the list of click entity for this scenario.
 */
internal data class ScenarioWithClicks(
    @Embedded val scenario: ScenarioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scenario_id"
    )
    val clicks: List<ClickEntity>
)

/**
 * Entity defining a condition to click.
 *
 * A condition is composed of a path and size of a bitmap to be matched on the screen, and the position of this matching
 * on the screen.
 *
 * It has a many to many relation with clicks, meaning that one click can have several conditions, and one condition
 * can be used by several clicks. This relationship is represented by the [ClickConditionCrossRef] entity.
 *
 * @param path the path on the application appData directory for the bitmap representing the condition. Also the
 *             primary key for this entity.
 * @param areaLeft the left coordinate of the rectangle defining the matching area.
 * @param areaTop the top coordinate of the rectangle defining the matching area.
 * @param areaRight the right coordinate of the rectangle defining the matching area.
 * @param areaBottom the bottom coordinate of the rectangle defining the matching area.
 * @param width the bitmap width. Should be the same as areaRight - areaLeft.
 * @param height the bitmap height. Should be the same as areaTop - areaBottom.
 * @param threshold the accepted difference between the conditions and the screen content, in percent (0-100%).
 */
@Entity(tableName = "condition_table")
internal data class ConditionEntity(
    @PrimaryKey(autoGenerate = false) @ColumnInfo(name= "path") val path: String,
    @ColumnInfo(name = "area_left") val areaLeft: Int,
    @ColumnInfo(name = "area_top") val areaTop: Int,
    @ColumnInfo(name = "area_right") val areaRight: Int,
    @ColumnInfo(name = "area_bottom") val areaBottom: Int,
    @ColumnInfo(name = "width") val width: Int,
    @ColumnInfo(name = "height") val height: Int,
    @ColumnInfo(name = "threshold", defaultValue = "1") val threshold: Int
)

/**
 * Cross reference entity between a [ClickEntity] and a [ConditionEntity].
 *
 * Conditions has a many to many relation with clicks, meaning that one click can have several conditions, and one
 * condition can be used by several clicks. For each one these relation, a cross reference entity is created, allowing
 * to match a click with its conditions and a conditions with the clicks.
 *
 * Deletion of a click or a condition associated with a ClickConditionCrossRef will lead to its automatic deletion, as
 * the relation is no longer valid.
 *
 * @param clickId the identifier of the click for this relation. Reference the key [ClickEntity.clickId] in click_table.
 * @param path the path of the condition for this relation. Reference the key [ConditionEntity.path] in condition_table.
 */
@Entity(
    primaryKeys = ["clickId", "path"],
    indices = [Index("clickId"), Index("path")],
    foreignKeys = [
        ForeignKey(
            entity = ClickEntity::class,
            parentColumns = ["clickId"],
            childColumns = ["clickId"],
            onDelete = CASCADE
        ),
        ForeignKey(
            entity = ConditionEntity::class,
            parentColumns = ["path"],
            childColumns = ["path"],
            onDelete = CASCADE
        )
    ])
internal data class ClickConditionCrossRef(
    val clickId: Long,
    val path: String
)

/**
 * Entity embedding a click and its conditions.
 *
 * Automatically do the junction between click_table and condition_table using [ClickConditionCrossRef] to provide
 * an entity containing a click and all of its conditions.
 *
 * @param click the click entity.
 * @param conditions the list of conditions entity for this click.
 */
internal data class ClickWithConditions(
    @Embedded val click: ClickEntity,
    @Relation(
        parentColumn = "clickId",
        entityColumn = "path",
        associateBy = Junction(ClickConditionCrossRef::class)
    )
    val conditions: List<ConditionEntity>
)
