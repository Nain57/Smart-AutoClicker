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
package com.buzbuz.smartautoclicker.database.utils

import com.buzbuz.smartautoclicker.database.room.ClickWithConditions
import com.buzbuz.smartautoclicker.database.room.ScenarioWithClicks

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.collection.IsIterableContainingInAnyOrder

import org.junit.Assert.assertEquals

/**
 * Asserts that two scenario list are equal, even if their order aren't the same in the list.
 * If they are not equals, an {@link AssertionError} is thrown.
 *
 * @param expected expected list of scenario.
 * @param actual the actual value from the database to be checked.
 */
internal fun assertSameScenarioList(expected: List<ScenarioWithClicks>, actual: List<ScenarioWithClicks>) {
    assertThat(actual.map { it.scenario }, IsIterableContainingInAnyOrder(expected.map { equalTo(it.scenario) }))
    actual.forEach { actualScenario ->
        val expectedScenario = expected.find { it.scenario.id == actualScenario.scenario.id }
        assertThat(actualScenario.clicks, IsIterableContainingInAnyOrder(expectedScenario!!.clicks.map { equalTo(it) }))
    }
}

/**
 * Asserts that two click list are the same.
 * They should be in the same order, with all values equals and the condition should be same, but not in the same order.
 * If they are not the same, an {@link AssertionError} is thrown.
 *
 * @param expected expected list of clicks.
 * @param actual the actual value from the database to be checked.
 */
internal fun assertSameClickList(expected: List<ClickWithConditions>, actual: List<ClickWithConditions>) {
    assertEquals("Click lists size aren't the same", expected.size, actual.size)
    actual.forEachIndexed { index, actualClick ->
        assertEquals("Click $index aren't the equals", expected[index].click, actualClick.click)
        assertThat("Conditions list $index isn't the same", actualClick.conditions,
            IsIterableContainingInAnyOrder(expected[index].conditions.map { equalTo(it) }))
    }
}
