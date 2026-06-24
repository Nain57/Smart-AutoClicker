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

const cv::Mat& DetectionImage::getHsvMat() const {
    if (!hsvValid && !colorMat.empty()) {
        cv::Mat rgbMat;
        cv::cvtColor(colorMat, rgbMat, cv::COLOR_RGBA2RGB);
        cv::cvtColor(rgbMat, hsvMat, cv::COLOR_RGB2HSV);
        hsvValid = true;
    }
    return hsvMat;
}

cv::Scalar DetectionImage::getHsvMean() const {
    const cv::Mat& hsv = getHsvMat();
    if (hsv.empty()) return {};
    return cv::mean(hsv);
}

cv::Rect DetectionImage::getRoi() const {
    return {0, 0, colorMat.cols, colorMat.rows};
}

bool DetectionImage::empty() const {
    return colorMat.empty();
}
