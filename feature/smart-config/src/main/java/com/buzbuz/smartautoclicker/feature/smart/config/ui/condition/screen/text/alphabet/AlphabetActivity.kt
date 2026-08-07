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
package com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import com.buzbuz.smartautoclicker.core.common.overlays.di.OverlaysEntryPoint
import com.buzbuz.smartautoclicker.core.common.overlays.manager.OverlayManager
import com.buzbuz.smartautoclicker.feature.smart.config.R
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.required.RequiredAlphabetFragment
import com.buzbuz.smartautoclicker.feature.smart.config.ui.condition.screen.text.alphabet.selection.AlphabetSelectionFragment
import dagger.hilt.EntryPoints

import dagger.hilt.android.AndroidEntryPoint

/** We use an activiy instead of an overlay here because the Play Asset Delivery requires a foreground Activity. */
@AndroidEntryPoint
class AlphabetActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_FRAGMENT_TAG =
            "com.buzbuz.smartautoclicker.feature.smart.config.ui.EXTRA_OCR_FRAGMENT_TAG"

        fun getStartIntent(context: Context, fragmentTag: String): Intent =
            Intent(context, AlphabetActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_FRAGMENT_TAG, fragmentTag)
    }

    private val overlayManager: OverlayManager by lazy {
        EntryPoints.get(applicationContext, OverlaysEntryPoint::class.java)
            .overlayManager()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transparent)

        overlayManager.hideAll()

        when (val tag = intent?.getStringExtra(EXTRA_FRAGMENT_TAG)) {
            AlphabetSelectionFragment.FRAGMENT_TAG ->
                AlphabetSelectionFragment().show(supportFragmentManager, AlphabetSelectionFragment.FRAGMENT_TAG)

            RequiredAlphabetFragment.FRAGMENT_TAG ->
                RequiredAlphabetFragment().show(supportFragmentManager, RequiredAlphabetFragment.FRAGMENT_TAG)

            else -> {
                Log.e(TAG, "Invalid fragment tag $tag")
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.restoreVisibility()
    }
}

private const val TAG = "OcrModelActivity"
