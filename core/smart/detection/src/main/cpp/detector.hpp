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

#ifndef KLICK_R_DETECTOR_HPP
#define KLICK_R_DETECTOR_HPP

#include <opencv2/imgproc/imgproc.hpp>

#include "matching/template_matcher.hpp"
#include "matching/template_matching_result.hpp"
#include "images/condition_image.hpp"
#include "images/screen_image.hpp"

namespace smartautoclicker {

    class Detector {

    private:
        double scaleRatio = 1;

        std::unique_ptr<ScreenImage> screenImage = std::make_unique<ScreenImage>();
        std::unique_ptr<TemplateMatcher> templateMatcher = std::make_unique<TemplateMatcher>();

    public:

        Detector() = default;

        void setScreenMetrics(cv::Mat* screenMat, double detectionQuality, const char *metricsTag);
        void setScreenImage(cv::Mat* screenMat);

        TemplateMatchingResult* detectCondition(cv::Mat* conditionMat, int threshold);
        TemplateMatchingResult* detectCondition(cv::Mat* conditionMat, int x, int y, int width, int height, int threshold);
    };
}

#endif //KLICK_R_DETECTOR_HPP