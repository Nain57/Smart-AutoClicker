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
package com.buzbuz.smartautoclicker.feature.billing.domain

import android.app.Activity

import com.buzbuz.smartautoclicker.feature.billing.IAdsRepository
import com.buzbuz.smartautoclicker.feature.billing.IBillingRepository
import com.buzbuz.smartautoclicker.feature.billing.data.ads.UserConsentDataSource

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AdsRepository @Inject internal constructor(
    private val billingRepository: IBillingRepository,
    private val userConsentDataSource: UserConsentDataSource,
) : IAdsRepository {

    override val isUserConsentingForAds: Flow<Boolean> = userConsentDataSource.isUserConsentingForAds
        .combine(billingRepository.isProModePurchased) { isConsenting, haveProMode ->
                !haveProMode && isConsenting
            }

    override val isPrivacyOptionsRequired: Flow<Boolean> = userConsentDataSource.isPrivacyOptionsRequired
        .combine(billingRepository.isProModePurchased) { required, haveProMode ->
                !haveProMode && required
            }

    override fun requestUserConsentIfNeeded(activity: Activity) {
            if (billingRepository.isPurchased()) return
            userConsentDataSource.requestUserConsent(activity)
        }

}