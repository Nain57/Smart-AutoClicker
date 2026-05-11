/*
 * Copyright (C) 2026 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.color.capture

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

import com.buzbuz.smartautoclicker.feature.smart.config.databinding.IncludeCardZoomedViewBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayColorCaptureZoomViewLandBinding
import com.buzbuz.smartautoclicker.feature.smart.config.databinding.OverlayColorCaptureZoomViewPortBinding

/** Overlays don't handle layout orientation folders for inflation, so use this abstraction. */
class OverlayColorCaptureZoomViewBinding private constructor (
    val rootView: View,
    val zoomCardPrimary: IncludeCardZoomedViewBinding,
    val zoomCardSecondary: IncludeCardZoomedViewBinding,
) : ViewBinding {

    companion object {
        fun inflate(layoutInflater: LayoutInflater, orientation: Int) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                OverlayColorCaptureZoomViewBinding(OverlayColorCaptureZoomViewPortBinding.inflate(layoutInflater))
            else
                OverlayColorCaptureZoomViewBinding(OverlayColorCaptureZoomViewLandBinding.inflate(layoutInflater))
    }

    constructor(binding: OverlayColorCaptureZoomViewPortBinding) : this(
        rootView = binding.root,
        zoomCardPrimary = binding.layoutZoomTop,
        zoomCardSecondary = binding.layoutZoomBottom,
    )

    constructor(binding: OverlayColorCaptureZoomViewLandBinding) : this(
        rootView = binding.root,
        zoomCardPrimary = binding.layoutZoomLeft,
        zoomCardSecondary = binding.layoutZoomRight,
    )

    override fun getRoot(): ViewGroup = rootView as ViewGroup
}