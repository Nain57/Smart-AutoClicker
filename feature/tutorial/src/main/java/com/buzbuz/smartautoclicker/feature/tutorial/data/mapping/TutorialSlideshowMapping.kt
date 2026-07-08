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
package com.buzbuz.smartautoclicker.feature.tutorial.data.mapping

import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.getScreenConditionsThresholdSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.getScreenConditionsTypeSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.getImageConditionsCaptureSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.image.getImageConditionsDetectionAreaSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.number.getNumberConditionsDetectionAreaSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.data.items.root.basics.screenconditions.text.getTextConditionsDetectionAreaSlideshow
import com.buzbuz.smartautoclicker.feature.tutorial.domain.model.TutorialSlideshow


internal fun TutorialSlideshow.Type.toTutorialSlideshow(): TutorialSlideshow =
    when (this) {
        TutorialSlideshow.Type.IMAGE_CONDITION_CAPTURE -> getImageConditionsCaptureSlideshow()
        TutorialSlideshow.Type.IMAGE_CONDITION_DETECTION_AREA -> getImageConditionsDetectionAreaSlideshow()
        TutorialSlideshow.Type.NUMBER_CONDITION_DETECTION_AREA -> getNumberConditionsDetectionAreaSlideshow()
        TutorialSlideshow.Type.SCREEN_CONDITIONS_DETECTION_THRESHOLD -> getScreenConditionsThresholdSlideshow()
        TutorialSlideshow.Type.SCREEN_CONDITIONS_TYPE -> getScreenConditionsTypeSlideshow()
        TutorialSlideshow.Type.TEXT_CONDITION_DETECTION_AREA -> getTextConditionsDetectionAreaSlideshow()
    }
