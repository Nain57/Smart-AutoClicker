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

#ifndef KLICK_R_TEXT_MATCHING_RESULT_HPP
#define KLICK_R_TEXT_MATCHING_RESULT_HPP

#include <opencv2/core/types.hpp>
#include "../../detection_result.hpp"

namespace smartautoclicker {

    class TextMatchingResult: public DetectionResult {
    private:
        bool detected;
        int centerX;
        int centerY;
        cv::Rect area;
        float recognizerConfidence;

    public:
        void updateResults(
                const cv::Rect& detectionArea,
                const cv::Rect& boundingBox,
                float confidence);
        void markResultAsDetected();
        void reset();

        [[nodiscard]] bool isDetected() const override;
        [[nodiscard]] double getResultConfidence() const override;
        [[nodiscard]] cv::Rect getResultArea() const override;
        [[nodiscard]] int getResultAreaCenterX() const override;
        [[nodiscard]] int getResultAreaCenterY() const override;
    };
} // smartautoclicker

#endif //KLICK_R_TEXT_MATCHING_RESULT_HPP
