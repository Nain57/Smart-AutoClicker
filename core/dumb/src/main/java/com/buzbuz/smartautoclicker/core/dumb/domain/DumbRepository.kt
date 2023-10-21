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
package com.buzbuz.smartautoclicker.core.dumb.domain

import android.content.Context

import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbDatabase
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbAction
import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario

import kotlinx.coroutines.flow.Flow

interface DumbRepository {

    companion object {

        /** Singleton preventing multiple instances of the repository at the same time. */
        @Volatile
        private var INSTANCE: DumbRepository? = null

        /**
         * Get the repository singleton, or instantiates it if it wasn't yet.
         *
         * @param context the Android context.
         *
         * @return the repository singleton.
         */
        fun getRepository(context: Context): DumbRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DumbRepositoryImpl(
                    DumbDatabase.getDatabase(context),
                )
                INSTANCE = instance
                instance
            }
        }
    }

    val dumbScenarios: Flow<List<DumbScenario>>

    suspend fun getDumbScenario(dbId: Long): DumbScenario?

    fun getDumbScenarioFlow(dbId: Long): Flow<DumbScenario?>

    fun getAllDumbActionsFlowExcept(scenarioDbId: Long): Flow<List<DumbAction>>

    suspend fun addDumbScenario(scenario: DumbScenario)

    suspend fun addDumbScenarioCopy(scenario: DumbScenarioWithActions): Long?

    suspend fun updateDumbScenario(scenario: DumbScenario)

    suspend fun deleteDumbScenario(scenario: DumbScenario)
}