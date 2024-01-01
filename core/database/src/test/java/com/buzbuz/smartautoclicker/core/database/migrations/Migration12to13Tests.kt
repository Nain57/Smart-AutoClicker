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
package com.buzbuz.smartautoclicker.core.database.migrations

import android.os.Build
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.buzbuz.smartautoclicker.core.database.ClickDatabase
import com.buzbuz.smartautoclicker.core.database.entity.ClickPositionType
import com.buzbuz.smartautoclicker.core.database.utils.assertCountEquals
import com.buzbuz.smartautoclicker.core.database.utils.assertRowIsV11Click
import com.buzbuz.smartautoclicker.core.database.utils.getV11Actions
import com.buzbuz.smartautoclicker.core.database.utils.insertV10Click
import com.buzbuz.smartautoclicker.core.database.utils.toExpectedV11Click
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tests the [Migration12to13]. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class Migration12to13Tests {

    private companion object {
        private const val TEST_DB = "migration-test"

        private const val OLD_DB_VERSION = 12
        private const val NEW_DB_VERSION = 13
    }

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ClickDatabase::class.java,
    )

    @Test
    fun migrate_fails() {
        // Given


        // Insert in v12 and close
        helper.createDatabase(TEST_DB, OLD_DB_VERSION).use { dbV12 ->

        }

        // Migrate to v13
        helper.runMigrationsAndValidate(TEST_DB, NEW_DB_VERSION, true, Migration12to13).use { dbV13 ->

        }
    }
}