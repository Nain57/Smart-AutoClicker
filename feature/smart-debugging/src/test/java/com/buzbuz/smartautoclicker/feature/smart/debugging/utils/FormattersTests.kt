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
package com.buzbuz.smartautoclicker.feature.smart.debugging.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTests {

    @Test
    fun `timeline phase duration always uses comparable milliseconds`() {
        assertEquals("+< 1", 999_999L.formatDebugTimelinePhaseDurationValue())
        assertEquals("+1.2", 1_234_567L.formatDebugTimelinePhaseDurationValue())
        assertEquals("+331.0", 331_000_000L.formatDebugTimelinePhaseDurationValue())
        assertEquals("+999.9", 999_949_999L.formatDebugTimelinePhaseDurationValue())
        assertEquals("+1000", 999_950_000L.formatDebugTimelinePhaseDurationValue())
        assertEquals("+1235", 1_234_567_890L.formatDebugTimelinePhaseDurationValue())
    }
}
