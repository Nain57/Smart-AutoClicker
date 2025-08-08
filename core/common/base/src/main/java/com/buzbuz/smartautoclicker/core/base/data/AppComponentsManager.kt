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