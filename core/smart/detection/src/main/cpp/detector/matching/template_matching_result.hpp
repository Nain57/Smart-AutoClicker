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

#ifndef KLICK_R_TEMPLATE_MATCHING_RESULT_HPP
#define KLICK_R_TEMPLATE_MATCHING_RESULT_HPP

#include <opencv2/core/types.hpp>

namespace smartautoclicker {

    class TemplateMatchingResult {
    private:
        bool detected;
        double minVal;
        double maxVal;
        cv::Point minLoc;
        cv::Point maxLoc;
        int centerX;
        int centerY;
        cv::Rect area;

    public:
        void updateResults(
                const cv::Rect& detectionArea,
                const cv::Mat& condition,
                cv::Mat& matchingResults);
        void markResultAsDetected();
        void reset();

        [[nodiscard]] bool isDetected() const;
        [[nodiscard]] double getResultConfidence() const;
        [[nodiscard]] cv::Rect getResultArea() const;
        [[nodiscard]] int getResultAreaCenterX() const;
        [[nodiscard]] int getResultAreaCenterY() const;

        void invalidateCurrentResult(const cv::Mat& condition, cv::Mat& results) const;
    };
} // smartautoclicker

#endif //KLICK_R_TEMPLATE_MATCHING_RESULT_HPP
