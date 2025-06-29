
#include "screen_image.hpp"

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

#include <opencv2/imgproc/imgproc.hpp>
#include "screen_image.hpp"
#include "../../utils/correction.hpp"

using namespace smartautoclicker;


void ScreenImage::processNewData(std::unique_ptr<cv::Mat> newData, const char* metricsTag) {
    if (!newData || newData->empty() || requiresCorrection(metricsTag)) return;

    this->colorMat = std::move(newData);
    cv::cvtColor(*colorMat, *grayMat, cv::COLOR_RGBA2GRAY);
}

cv::Mat ScreenImage::cropColor(const cv::Rect &roi) const {
    if (!this->colorMat || this->colorMat->empty()) return {};
    return cropMat(*this->colorMat, roi);
}

cv::Mat ScreenImage::cropGray(const cv::Rect &roi) const {
    if (!this->grayMat || this->grayMat->empty()) return {};
    return cropMat(*this->grayMat, roi);
}

cv::Mat ScreenImage::cropMat(const cv::Mat& mat, const cv::Rect& roi) {
    cv::Rect imageBounds(0, 0, mat.cols, mat.rows);
    cv::Rect validRoi = roi & imageBounds;

    if (validRoi.width <= 0 || validRoi.height <= 0) return {};

    return (mat)(validRoi);
}
