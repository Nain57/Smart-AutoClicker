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
#include "detection_image.hpp"

using namespace smartautoclicker;


const cv::Mat& DetectionImage::getColorMat() const {
    return colorMat;
}

const cv::Mat& DetectionImage::getGrayMat() const {
    if (!grayValid && !colorMat.empty()) {
        cv::cvtColor(colorMat, grayMat, cv::COLOR_RGBA2GRAY);
        grayValid = true;
    }
    return grayMat;
}

cv::Rect DetectionImage::getRoi() const {
    return {0, 0, colorMat.cols, colorMat.rows};
}

bool DetectionImage::empty() const {
    return colorMat.empty();
}
