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
package com.buzbuz.smartautoclicker.feature.tutorial.domain

import com.buzbuz.smartautoclicker.feature.tutorial.data.game.TutorialGamesDataSource
import com.buzbuz.smartautoclicker.feature.tutorial.domain.game.TutorialGame

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class TutorialRepository(gamesDataSource: TutorialGamesDataSource) {

    companion object {

        /** Singleton preventing multiple instances of the TutorialRepository at the same time. */
        @Volatile
        private var INSTANCE: TutorialRepository? = null

        /**
         * Get the TutorialRepository singleton, or instantiates it if it wasn't yet.
         *
         * @return the TutorialRepository singleton.
         */
        fun getTutorialRepository(): TutorialRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TutorialRepository(TutorialGamesDataSource)
                INSTANCE = instance
                instance
            }
        }
    }

    private val games: List<TutorialGame> = gamesDataSource.tutorialGames

    private val currentGameIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val currentGame: Flow<TutorialGame> = currentGameIndex.map { gameIndex -> games[gameIndex] }

    fun setGameIndex(index: Int) {
        if (index < 0 || index >= games.size) return
        currentGameIndex.value = index
    }

    fun nextGame() {
        val gameIndex = currentGameIndex.value + 1
        if (gameIndex < 0 || gameIndex >= games.size) return

        currentGameIndex.value = gameIndex
    }
}