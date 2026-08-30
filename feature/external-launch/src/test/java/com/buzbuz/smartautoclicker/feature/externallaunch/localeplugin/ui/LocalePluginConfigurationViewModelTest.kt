/*
 * Copyright (C) 2026 Kevin Buzeau
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.ui

import androidx.appcompat.app.AppCompatActivity
import com.buzbuz.smartautoclicker.core.common.permissions.PermissionsController
import com.buzbuz.smartautoclicker.core.common.permissions.model.Permission
import com.buzbuz.smartautoclicker.core.common.permissions.model.PermissionPostNotification
import com.buzbuz.smartautoclicker.core.domain.IRepository
import com.buzbuz.smartautoclicker.core.dumb.domain.DumbRepository
import com.buzbuz.smartautoclicker.feature.externallaunch.localeplugin.domain.LocalePluginConfigurationCodec
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalePluginConfigurationViewModelTest {

    private val smartRepository = mockk<IRepository>(relaxed = true)
    private val dumbRepository = mockk<DumbRepository>(relaxed = true)
    private val codec = mockk<LocalePluginConfigurationCodec>()
    private val permissionsController = mockk<PermissionsController>(relaxed = true)
    private val viewModel = LocalePluginConfigurationViewModel(
        smartRepository = smartRepository,
        dumbRepository = dumbRepository,
        codec = codec,
        permissionController = permissionsController,
    )

    @Test
    fun `saving a Locale launch setup requests notification fallback permission`() {
        val permissions = slot<List<Permission>>()

        viewModel.requestFallbackNotificationPermission(mockk<AppCompatActivity>(), onGranted = {})

        verify {
            permissionsController.startPermissionsUiFlow(
                activity = any(),
                permissions = capture(permissions),
                onAllGranted = any(),
                onMandatoryDenied = null,
            )
        }
        val permission = permissions.captured.single() as PermissionPostNotification
        assertEquals(PermissionPostNotification.Purpose.EXTERNAL_LAUNCH_FALLBACK, permission.purpose)
    }
}
