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
package com.buzbuz.smartautoclicker.feature.backup.data.smart

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartBackupDataSourceTests {

    @Test
    fun sameScreenSize_sameOrientation_noWarning() {
        assertFalse(hasDifferentScreenSize(1080, 2400, 1080, 2400))
    }

    @Test
    fun sameScreenSize_differentOrientation_noWarning() {
        assertFalse(hasDifferentScreenSize(1080, 2400, 2400, 1080))
    }

    @Test
    fun differentScreenSize_warning() {
        assertTrue(hasDifferentScreenSize(1080, 2400, 1440, 3200))
    }
}
