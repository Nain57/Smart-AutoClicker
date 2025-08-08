
package com.buzbuz.smartautoclicker.scenarios.list.copy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.buzbuz.smartautoclicker.core.base.di.Dispatcher
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.IO
import com.buzbuz.smartautoclicker.core.base.di.HiltCoroutineDispatchers.Main
import com.buzbuz.smartautoclicker.core.domain.IRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ScenarioCopyViewModel @Inject constructor(
    @Dispatcher(Main) private val mainDispatcher: CoroutineDispatcher,
    @Dispatcher(IO) private val ioDispatcher: CoroutineDispatcher,
    private val smartRepository: IRepository,
) : ViewModel() {

    private val copyName: MutableStateFlow<String?> = MutableStateFlow(null)
    val copyNameError: Flow<Boolean> = copyName
        .map { it.isNullOrEmpty() }

    fun setCopyName(name: String) {
        copyName.value = name
    }

    fun copyScenario(scenarioId: Long, isSmart: Boolean, onCompleted: () -> Unit) {
        val name = copyName.value
        if (name.isNullOrEmpty()) return

        viewModelScope.launch(ioDispatcher) {
            smartRepository.addScenarioCopy(scenarioId, name)

            withContext(mainDispatcher) {
                onCompleted()
            }
        }
    }
}