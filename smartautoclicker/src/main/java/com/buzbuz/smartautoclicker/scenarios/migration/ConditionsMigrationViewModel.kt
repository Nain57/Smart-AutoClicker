/*
 * Copyright (C) 2025 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.scenarios.migration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buzbuz.smartautoclicker.R

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.ui.bindings.buttons.LoadableButtonState

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConditionsMigrationViewModel @Inject constructor(
    @ApplicationContext context: Context,
    @param:Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val smartRepository: IRepository,
) : ViewModel() {

    private val migrationState: MutableStateFlow<MigrationState> = MutableStateFlow(MigrationState.NOT_STARTED)
    private val conditionsToMigrate: Flow<Int> = smartRepository.legacyConditionsCount

    val uiState: Flow<ConditionsMigrationUiState> =
        combine(migrationState, conditionsToMigrate) { state, conditionCount ->
            context.getUiState(state, conditionCount)
        }

    fun startMigration() {
        if (migrationState.value != MigrationState.NOT_STARTED) return

        viewModelScope.launch(ioDispatcher) {
            migrationState.update { MigrationState.STARTED }

            val isSuccess = smartRepository.migrateLegacyImageConditions()
            migrationState.update {
                if (isSuccess) MigrationState.FINISHED
                else MigrationState.FINISHED_WITH_ERROR
            }
        }
    }
}

private fun Context.getUiState(state: MigrationState, conditionsCount: Int): ConditionsMigrationUiState =
    when (state) {
        MigrationState.NOT_STARTED ->
            ConditionsMigrationUiState(
                migrationState = state,
                textState = getString(R.string.message_condition_migration_count, conditionsCount),
                buttonState = LoadableButtonState.Loaded.Enabled(getString(R.string.button_condition_migration_start))
            )

        MigrationState.STARTED ->
            ConditionsMigrationUiState(
                migrationState = state,
                textState = getString(R.string.message_condition_migration_count, conditionsCount),
                buttonState = LoadableButtonState.Loading(getString(R.string.button_condition_migration_running))
            )

        MigrationState.FINISHED ->
            ConditionsMigrationUiState(
                migrationState = state,
                textState = getString(R.string.message_condition_migration_success),
                buttonState = LoadableButtonState.Loaded.Enabled(getString(R.string.button_condition_migration_finished))
            )

        MigrationState.FINISHED_WITH_ERROR ->
            ConditionsMigrationUiState(
                migrationState = state,
                textState = getString(R.string.message_condition_migration_error),
                buttonState = LoadableButtonState.Loaded.Enabled(getString(R.string.button_condition_migration_finished))
            )
    }

data class ConditionsMigrationUiState(
    val migrationState: MigrationState,
    val textState: String,
    val buttonState: LoadableButtonState,
)

enum class MigrationState {
    NOT_STARTED,
    STARTED,
    FINISHED,
    FINISHED_WITH_ERROR,
}