/*
 * Copyright (C) 2022 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.core.database.migrations

import androidx.room.DeleteColumn
import androidx.room.migration.AutoMigrationSpec

/**
 * Migration from database v8 to v9.
 *
 * Changes:
 * * removes the stop_after value from event_table. It was unused.
 * * add randomize column to scenario_table.
 * * add toggle_event_id and toggle_type to action_table.
 */
@DeleteColumn(tableName = "event_table", columnName = "stop_after")
class AutoMigration8to9 : AutoMigrationSpec