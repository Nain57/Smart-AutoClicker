
package com.buzbuz.smartautoclicker.scenarios.list.sort

data class ScenarioSortConfig(
    val type: ScenarioSortType,
    val inverted: Boolean,
    val showSmartScenario: Boolean,
)

enum class ScenarioSortType {
    NAME,
    RECENT,
    MOST_USED,
}

