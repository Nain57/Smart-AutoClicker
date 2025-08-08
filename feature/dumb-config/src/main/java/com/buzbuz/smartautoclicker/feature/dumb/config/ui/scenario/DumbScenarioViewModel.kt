
package com.buzbuz.smartautoclicker.feature.dumb.config.ui.scenario

import androidx.lifecycle.ViewModel

import com.buzbuz.smartautoclicker.core.dumb.domain.model.DumbScenario
import com.buzbuz.smartautoclicker.feature.dumb.config.R
import com.buzbuz.smartautoclicker.feature.dumb.config.domain.DumbEditionRepository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DumbScenarioViewModel @Inject constructor(
    dumbEditionRepository: DumbEditionRepository,
) : ViewModel() {

    private val userModifications: StateFlow<DumbScenario?> =
        dumbEditionRepository.editedDumbScenario

    /**
     * Tells if all content have their field correctly configured.
     * Used to display the red badge if indicating if there is something missing.
     */
    val navItemsValidity: Flow<Map<Int, Boolean>> = userModifications.map { dumbScenario ->
        buildMap {
            put(R.id.page_actions, dumbScenario?.dumbActions?.isNotEmpty() ?: false)
            put(R.id.page_config, dumbScenario?.name?.isNotEmpty() ?: false)
        }
    }

    /** Tells if the configured event is valid and can be saved. */
    val canBeSaved: Flow<Boolean> = userModifications.map { dumbScenario ->
        dumbScenario?.isValid() == true
    }
}
