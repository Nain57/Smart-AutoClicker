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
package com.buzbuz.smartautoclicker.feature.tutorial.data

import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.newOneMovingTargetTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.newOneStillTargetTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.newTwoMovingTargetsPressInOrderTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.newTwoStillTargetsPressWhenBothVisibleTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.newTwoStillTargetsPressWhenOneVisibleTutorial
import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.oneMovingTargetTutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.oneStillTargetTutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.twoMovingTargetsPressInOrderTutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.twoStillTargetsPressWhenBothVisibleTutorialInfo
import com.buzbuz.smartautoclicker.feature.tutorial.data.tutorials.twoStillTargetsPressWhenOneVisibleTutorialInfo

internal object TutorialDataSource {

    val tutorialsInfo: List<TutorialInfo> = listOf(
        oneStillTargetTutorialInfo,
        oneMovingTargetTutorialInfo,
        twoStillTargetsPressWhenBothVisibleTutorialInfo,
        twoStillTargetsPressWhenOneVisibleTutorialInfo,
        twoMovingTargetsPressInOrderTutorialInfo,
    )

    fun getTutorialData(index: Int): TutorialData? {
        if (index < 0 || index > tutorialsInfo.lastIndex) return null

        return when (tutorialsInfo[index]) {
            oneStillTargetTutorialInfo -> newOneStillTargetTutorial()
            oneMovingTargetTutorialInfo -> newOneMovingTargetTutorial()
            twoStillTargetsPressWhenBothVisibleTutorialInfo -> newTwoStillTargetsPressWhenBothVisibleTutorial()
            twoStillTargetsPressWhenOneVisibleTutorialInfo -> newTwoStillTargetsPressWhenOneVisibleTutorial()
            twoMovingTargetsPressInOrderTutorialInfo -> newTwoMovingTargetsPressInOrderTutorial()
            else -> null
        }
    }
}