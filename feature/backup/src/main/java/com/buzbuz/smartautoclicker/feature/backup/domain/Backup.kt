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
package com.buzbuz.smartautoclicker.feature.backup.domain

/** State of a backup import/export. */
internal sealed class Backup {

    /**
     * The backup is progressing.
     * @param progress the current scenario count, if available.
     * @param maxProgress the maximum progress value, if available.
     */
    data class Loading(val progress: Int? = null, val maxProgress: Int? = null) : Backup()

    /** The backup is verifying the imported data. */
    data object Verification : Backup()

    /**
     * The backup is completed.
     * @param successCount the number of scenario successfully imported/exported.
     * @param failureCount the number of failed import/export.
     * @param compatWarning true if the screen size of an imported scenario is different than this device screen size.
     */
    data class Completed(val successCount: Int, val failureCount: Int, val compatWarning: Boolean) : Backup()

    /** The backup has encountered an error and has stopped. */
    data object Error : Backup()
}