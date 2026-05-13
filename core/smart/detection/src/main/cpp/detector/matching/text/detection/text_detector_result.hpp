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

#ifndef KLICK_R_TEXT_DETECTOR_RESULT_HPP
#define KLICK_R_TEXT_DETECTOR_RESULT_HPP

#include <opencv2/core.hpp>

namespace smartautoclicker {

    /**
     * Output result for the TextDetector class.
     * Note: crop Mat references the original cropped rgb Mat to avoid cloning overhead
     */
    struct TextDetectorResult {
        TextDetectorResult() = default;
        TextDetectorResult(const cv::Rect& box, cv::Mat boxCrop) : boundingBox(box), crop(std::move(boxCrop)) {}

        /** Box around detected text. */
        cv::Rect boundingBox;
        /** RGB view of the text within the screenCrop. */
        cv::Mat crop;
    };

}

#endif //KLICK_R_TEXT_DETECTOR_RESULT_HPP
