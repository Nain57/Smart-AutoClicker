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
package com.buzbuz.smartautoclicker.feature.backup.data

import com.buzbuz.smartautoclicker.core.database.entity.CompleteScenario
import com.buzbuz.smartautoclicker.core.dumb.data.database.DumbScenarioWithActions

/**
 * Notifies for a backup progress and state.
 *
 * @param onProgressChanged called for each scenario processed.
 * @param onCompleted called when the backup is completed.
 * @param onError called when the backup has encountered an error.
 * @param onVerification called when the backup is verifying the imported data.
 */
class BackupProgress(
    val onProgressChanged: suspend (current: Int?, max: Int?) -> Unit,
    val onCompleted: suspend (
        dumbScenario: List<DumbScenarioWithActions>,
        smartScenario: List<CompleteScenario>,
        failureCount: Int,
        compatWarning: Boolean,
    ) -> Unit,
    val onError: suspend () -> Unit,
    val onVerification: (suspend () -> Unit)? = null,
)