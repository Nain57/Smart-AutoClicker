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
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.BOOLEAN,
                value = "true"
            ),
            ActionTestsData.getNewIntentExtra(value = true).toEntity()
        )
    }

    @Test
    fun toIntentExtra_boolean() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtra(value = true),
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.BOOLEAN,
                value = "true"
            ).toDomainIntentExtra(),
        )
    }

    @Test
    fun toEntity_byte() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.BYTE,
                value = "21"
            ),
            ActionTestsData.getNewIntentExtra<Byte>(value = 21).toEntity()
        )
    }

    @Test
    fun toIntentExtra_byte() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtra<Byte>(value = 16),
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.BYTE,
                value = "16"
            ).toDomainIntentExtra(),
        )
    }

    @Test
    fun toEntity_char() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.CHAR,
                value = "a"
            ),
            ActionTestsData.getNewIntentExtra(value = 'a').toEntity()
        )
    }

    @Test
    fun toIntentExtra_char() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtra(value = 'a'),
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.CHAR,
                value = "a"
            ).toDomainIntentExtra(),
        )
    }

    @Test
    fun toEntity_double() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.DOUBLE,
                value = "2.0"
            ),
            ActionTestsData.getNewIntentExtra(value = 2.0).toEntity()
        )
    }

    @Test
    fun toIntentExtra_double() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtra(value = 2.0),
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.DOUBLE,
                value = "2.0"
            ).toDomainIntentExtra(),
        )
    }

    @Test
    fun toEntity_integer() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.INTEGER,
                value = "21"
            ),
            ActionTestsData.getNewIntentExtra(value = 21).toEntity()
        )
    }

    @Test
    fun toIntentExtra_integer() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtra(value = 21),
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.INTEGER,
                value = "21"
            ).toDomainIntentExtra(),
        )
    }

    @Test
    fun toEntity_float() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.FLOAT,
                value = "2.0"
            ),
            ActionTestsData.getNewIntentExtra(value = 2.0f).toEntity()
        )
    }

    @Test
    fun toIntentExtra_float() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtra(value = 2.0f),
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.FLOAT,
                value = "2.0"
            ).toDomainIntentExtra(),
        )
    }

    @Test
    fun toEntity_short() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.SHORT,
                value = "2"
            ),
            ActionTestsData.getNewIntentExtra<Short>(value = 2).toEntity()
        )
    }

    @Test
    fun toIntentExtra_short() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtra<Short>(value = 2),
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.SHORT,
                value = "2"
            ).toDomainIntentExtra(),
        )
    }

    @Test
    fun toEntity_string() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.STRING,
                value = "2"
            ),
            ActionTestsData.getNewIntentExtra(value = "2").toEntity()
        )
    }

    @Test
    fun toIntentExtra_string() {
        Assert.assertEquals(
            ActionTestsData.getNewIntentExtra(value = "2"),
            ActionTestsData.getNewIntentExtraEntity(
                type = IntentExtraType.STRING,
                value = "2"
            ).toDomainIntentExtra(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun toEntity_invalidType() {
        ActionTestsData.getNewIntentExtra(value = Point()).toEntity()
    }
}