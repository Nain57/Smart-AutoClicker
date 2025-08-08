
package com.buzbuz.smartautoclicker.core.base.data

import android.content.ComponentName
import javax.inject.Inject
import javax.inject.Singleton


interface AppComponentsProvider {
    val originalAppId: String

    val klickrServiceComponentName: ComponentName
    val scenarioActivityComponentName: ComponentName
}


@Singleton
class AppComponentsManager @Inject constructor() : AppComponentsProvider {

    private lateinit var _originalAppId: String
    override val originalAppId: String
        get() = _originalAppId

    private lateinit var _klickrServiceComponentName: ComponentName
    override val klickrServiceComponentName: ComponentName
        get() = _klickrServiceComponentName

    private lateinit var _scenarioActivityComponentName: ComponentName
    override val scenarioActivityComponentName: ComponentName
        get() = _scenarioActivityComponentName

    fun registerOriginalAppId(appId: String) {
        _originalAppId = appId
    }

    fun registerScenarioActivity(componentName: ComponentName) {
        _scenarioActivityComponentName = componentName
    }

    fun registerSmartAutoClickerService(componentName: ComponentName) {
        _klickrServiceComponentName = componentName
    }
}