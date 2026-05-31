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
#include "condition_image.hpp"

using namespace smartautoclicker;

void ConditionImage::processNewData(std::unique_ptr<cv::Mat> newData, int targetWidth, int targetHeight) {
    if (!newData || newData->empty()) return;

    if (newData->cols == targetWidth && newData->rows == targetHeight) {
        this->colorMat = std::move(*newData);
    } else {
        cv::resize(
                *newData,
                this->colorMat,
                cv::Size(targetWidth, targetHeight),
                0, 0,
                cv::INTER_AREA);
    }

    grayValid = false;
}

cv::Scalar ConditionImage::getColorMean() const {
    if (colorMat.empty()) return {};
    return cv::mean(colorMat);
}
