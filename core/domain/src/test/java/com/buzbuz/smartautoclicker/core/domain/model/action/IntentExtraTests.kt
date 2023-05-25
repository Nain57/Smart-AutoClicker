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
package com.buzbuz.smartautoclicker.core.domain.model.action

import android.graphics.Point
import android.os.Build

import androidx.test.ext.junit.runners.AndroidJUnit4

import com.buzbuz.smartautoclicker.core.database.entity.IntentExtraType
import com.buzbuz.smartautoclicker.core.domain.utils.TestsData

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

import org.robolectric.annotation.Config

/** Tests the [IntentExtra] class. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class IntentExtraTests {

    @Test
    fun toEntity_boolean() {
        Assert.assertEquals(
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.BOOLEAN,
                value = "true"
            ),
            TestsData.getNewIntentExtra(value = true).toEntity()
        )
    }

    @Test
    fun toIntentExtra_boolean() {
        Assert.assertEquals(
            TestsData.getNewIntentExtra(value = true),
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.BOOLEAN,
                value = "true"
            ).toIntentExtra(),
        )
    }

    @Test
    fun toEntity_byte() {
        Assert.assertEquals(
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.BYTE,
                value = "21"
            ),
            TestsData.getNewIntentExtra<Byte>(value = 21).toEntity()
        )
    }

    @Test
    fun toIntentExtra_byte() {
        Assert.assertEquals(
            TestsData.getNewIntentExtra<Byte>(value = 16),
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.BYTE,
                value = "16"
            ).toIntentExtra(),
        )
    }

    @Test
    fun toEntity_char() {
        Assert.assertEquals(
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.CHAR,
                value = "a"
            ),
            TestsData.getNewIntentExtra(value = 'a').toEntity()
        )
    }

    @Test
    fun toIntentExtra_char() {
        Assert.assertEquals(
            TestsData.getNewIntentExtra(value = 'a'),
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.CHAR,
                value = "a"
            ).toIntentExtra(),
        )
    }

    @Test
    fun toEntity_double() {
        Assert.assertEquals(
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.DOUBLE,
                value = "2.0"
            ),
            TestsData.getNewIntentExtra(value = 2.0).toEntity()
        )
    }

    @Test
    fun toIntentExtra_double() {
        Assert.assertEquals(
            TestsData.getNewIntentExtra(value = 2.0),
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.DOUBLE,
                value = "2.0"
            ).toIntentExtra(),
        )
    }

    @Test
    fun toEntity_integer() {
        Assert.assertEquals(
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.INTEGER,
                value = "21"
            ),
            TestsData.getNewIntentExtra(value = 21).toEntity()
        )
    }

    @Test
    fun toIntentExtra_integer() {
        Assert.assertEquals(
            TestsData.getNewIntentExtra(value = 21),
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.INTEGER,
                value = "21"
            ).toIntentExtra(),
        )
    }

    @Test
    fun toEntity_float() {
        Assert.assertEquals(
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.FLOAT,
                value = "2.0"
            ),
            TestsData.getNewIntentExtra(value = 2.0f).toEntity()
        )
    }

    @Test
    fun toIntentExtra_float() {
        Assert.assertEquals(
            TestsData.getNewIntentExtra(value = 2.0f),
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.FLOAT,
                value = "2.0"
            ).toIntentExtra(),
        )
    }

    @Test
    fun toEntity_short() {
        Assert.assertEquals(
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.SHORT,
                value = "2"
            ),
            TestsData.getNewIntentExtra<Short>(value = 2).toEntity()
        )
    }

    @Test
    fun toIntentExtra_short() {
        Assert.assertEquals(
            TestsData.getNewIntentExtra<Short>(value = 2),
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.SHORT,
                value = "2"
            ).toIntentExtra(),
        )
    }

    @Test
    fun toEntity_string() {
        Assert.assertEquals(
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.STRING,
                value = "2"
            ),
            TestsData.getNewIntentExtra(value = "2").toEntity()
        )
    }

    @Test
    fun toIntentExtra_string() {
        Assert.assertEquals(
            TestsData.getNewIntentExtra(value = "2"),
            TestsData.getNewIntentExtraEntity(
                type = IntentExtraType.STRING,
                value = "2"
            ).toIntentExtra(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun toEntity_invalidType() {
        TestsData.getNewIntentExtra(value = Point()).toEntity()
    }
}