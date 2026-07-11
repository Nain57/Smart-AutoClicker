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
#include <map>

#include "matching/color/color_matcher.hpp"
#include "matching/color/color_matching_result.hpp"
#include "matching/template/template_matcher.hpp"
#include "matching/template/template_matching_result.hpp"
#include "matching/text/text_matcher.hpp"
#include "matching/text/text_matching_result.hpp"
#include "images/condition_image.hpp"
#include "images/screen_image.hpp"

namespace smartautoclicker {

    class Detector {

    private:
        std::unique_ptr<ScreenImage> screenImage = std::make_unique<ScreenImage>();
        std::unique_ptr<ConditionImage> conditionImage = std::make_unique<ConditionImage>();

        std::unique_ptr<ColorMatcher> colorMatcher = std::make_unique<ColorMatcher>();
        std::unique_ptr<TemplateMatcher> templateMatcher = std::make_unique<TemplateMatcher>();
        std::unique_ptr<TextMatcher> textMatcher = std::make_unique<TextMatcher>();

    public:

        Detector() = default;

        bool loadModels(const std::string& detectionModelPath, const std::map<std::string, std::string>& recognitionModels);
        void setScreenImage(std::unique_ptr<cv::Mat> screenColorMat, const char* metricsTag);

        TemplateMatchingResult* detectImage(
                std::unique_ptr<cv::Mat> conditionMat,
                int targetConditionWidth,
                int targetConditionHeight,
                const cv::Rect& roi,
                int threshold);

        ColorMatchingResult* detectColor(
                int colorCondition,
                const cv::Rect& roi,
                int threshold);

        TextMatchingResult* detectText(
                const char* textCondition,
                const char* recognitionModelId,
                const cv::Rect &roi,
                int threshold);

        TextMatchingResult* detectNumber(const cv::Rect& roi, int threshold, NumberFormat numberFormat);
    };
}

#endif //KLICK_R_DETECTOR_HPP