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

#ifndef KLICK_R_COLOR_MATCHER_HPP
#define KLICK_R_COLOR_MATCHER_HPP

#include "color_matching_result.hpp"
#include "../../images/condition_image.hpp"
#include "../../images/screen_image.hpp"

namespace smartautoclicker {

    class ColorMatcher {

    private:
        ColorMatchingResult currentMatchingResult;

    public:
        void reset();
        static bool isRoiValidForMatching(const cv::Rect& screenRoi, const cv::Rect& roi);
        void matchColor(
                const ScreenImage& screenImage,
                const cv::Scalar& conditionColor,
                const cv::Rect& detectionArea,
                int threshold);

        ColorMatchingResult* getMatchingResults();

    };
}

#endif //KLICK_R_COLOR_MATCHER_HPP
