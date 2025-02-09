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

#ifndef KLICK_R_MATCHING_RESULTS_HPP
#define KLICK_R_MATCHING_RESULTS_HPP

#include <opencv2/core/types.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include "../types/scalable_roi.hpp"

namespace smartautoclicker {

    class MatchingResults {

    private:
        std::unique_ptr<cv::Mat> templateMatchingResult = std::make_unique<cv::Mat>();
        std::unique_ptr<cv::Mat> minMaxMask = std::make_unique<cv::Mat>();

    public:
        double minVal;
        double maxVal;
        cv::Point minLoc = cv::Point(0, 0);
        cv::Point maxLoc = cv::Point(0, 0);
        ScalableRoi roi;

        cv::Mat* initResults(const cv::Mat& screenImage, const cv::Mat& conditionImage);
        void locateNextMinMax(const cv::Mat& conditionImage, double scaleRatio);
    };
}

#endif //KLICK_R_MATCHING_RESULTS_HPP
