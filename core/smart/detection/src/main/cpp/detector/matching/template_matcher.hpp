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

#ifndef KLICK_R_TEMPLATE_MATCHER_HPP
#define KLICK_R_TEMPLATE_MATCHER_HPP

#include <opencv2/core/types.hpp>

#include "../images/condition_image.hpp"
#include "../images/screen_image.hpp"
#include "../../scaling/scalable_roi.hpp"
#include "template_matching_result.hpp"

namespace smartautoclicker {

    class TemplateMatcher {

    private:
        TemplateMatchingResult currentMatchingResult;

        void parseMatchingResult(
                cv::Mat* matchingResult,
                ScreenImage* screenImage,
                ConditionImage* condition,
                ScalableRoi* detectionArea,
                double scaleRatio,
                int threshold);

        static bool isConfidenceValid(double confidence, int threshold);
        static double getColorDiff(const cv::Mat& image, const cv::Scalar& conditionColorMeans);

    public:
        void reset();
        void matchTemplate(
                ScreenImage* screenImage,
                ConditionImage* condition,
                ScalableRoi* detectionArea,
                double scalingRatio,
                int threshold);

        DetectionResult* getMatchingResults();

    };
}

#endif //KLICK_R_TEMPLATE_MATCHER_HPP
