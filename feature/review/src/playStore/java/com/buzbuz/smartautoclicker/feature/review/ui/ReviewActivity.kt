/*
 * Copyright (C) 2024 Kevin Buzeau
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
package com.buzbuz.smartautoclicker.feature.review.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.buzbuz.smartautoclicker.feature.review.R

import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ReviewActivity : AppCompatActivity() {

    private val viewModel: ReviewViewModel by viewModels()

    private val reviewManager: ReviewManager by lazy {
        ReviewManagerFactory.create(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!viewModel.isUserCandidateForReview()) {
            Log.w(TAG, "User is not candidate for review, activity will stop")
            finish()
            return
        }
        setContentView(R.layout.activity_review)

        Log.i(TAG, "Requesting ReviewInfo")
        reviewManager.requestReviewFlow()
            .addOnCompleteListener(::onReviewInfoTaskCompleted)
    }

    private fun onReviewInfoTaskCompleted(reviewInfoTask: Task<ReviewInfo>) {
        if (!reviewInfoTask.isSuccessful) {
            Log.w(TAG, "Can't request ReviewInfo: ${reviewInfoTask.exception}")
            finish()
            return
        }

        Log.i(TAG, "Starting review UI flow with ReviewInfo: ${reviewInfoTask.result}")
        reviewManager.launchReviewFlow(this, reviewInfoTask.result)
            .addOnCompleteListener { onReviewUiFlowCompleted() }
    }

    private fun onReviewUiFlowCompleted() {
        Log.i(TAG, "Review UI flow completed")

        viewModel.updateLastReviewRequestTimestamp()
        finish()
    }
}

private const val TAG = "ReviewActivity"
