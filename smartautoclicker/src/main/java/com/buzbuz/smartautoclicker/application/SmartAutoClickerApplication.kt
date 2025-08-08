
package com.buzbuz.smartautoclicker.application

import android.app.Application
import com.buzbuz.smartautoclicker.ComponentConfig
import com.buzbuz.smartautoclicker.core.base.data.AppComponentsManager
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SmartAutoClickerApplication : Application() {

    @Inject lateinit var appComponentsManager: AppComponentsManager

    override fun onCreate() {
        super.onCreate()

        val componentConfig = ComponentConfig
        appComponentsManager.apply {
            registerOriginalAppId(componentConfig.ORIGINAL_APP_ID)
            registerSmartAutoClickerService(componentConfig.smartAutoClickerService)
            registerScenarioActivity(componentConfig.scenarioActivity)
        }

        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}