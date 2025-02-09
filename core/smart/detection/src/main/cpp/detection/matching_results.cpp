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

#include <opencv2/core/types.hpp>
#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/imgproc/imgproc.hpp>
#include "matching_results.hpp"
#include "../utils/log.h"

using namespace smartautoclicker;


cv::Mat* MatchingResults::initResults(const cv::Mat& screenImage, const cv::Mat& conditionImage) {
    // Reset previous results
    minVal = 0.0;
    maxVal = 0.0;
    minLoc.x = 0;
    minLoc.y = 0;
    maxLoc.x = 0;
    maxLoc.y = 0;
    roi.clear();

    // Scale the result matrix for the new template matching inputs
    templateMatchingResult->create(
            std::max(screenImage.rows - conditionImage.rows + 1, 0),
            std::max(screenImage.cols - conditionImage.cols + 1, 0),
            CV_32F);

    return templateMatchingResult.get();
}

void MatchingResults::locateNextMinMax(const cv::Mat& conditionImage, double scaleRatio) {
    // If we already located a previous min/max on those results, set its roi to 0 in template results,
    // so it won't be found again.
    if (!roi.isEmpty()) {
        cv::rectangle(*templateMatchingResult, roi.scaled, cv::Scalar(0), CV_FILLED);
    }

    // Find the best result
    cv::minMaxLoc(*templateMatchingResult, &minVal, &maxVal, &minLoc, &maxLoc, cv::noArray());

    // Update the result location roi
    roi.setScaled(maxLoc.x, maxLoc.y, conditionImage.cols, conditionImage.rows, scaleRatio);
}
