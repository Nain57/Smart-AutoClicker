/*
 * Copyright (C) 2024 Kevin Buzeau
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
#include "scalable_roi.hpp"
#include "../logs/log.h"

using namespace smartautoclicker;

void ScalableRoi::setFullSize(cv::Mat* fullSize, double scaleRatio) {
    fullSizeRoi.x = 0;
    fullSizeRoi.y = 0;
    fullSizeRoi.width = fullSize->cols;
    fullSizeRoi.height = fullSize->rows;

    scaledRoi.x = cvRound(fullSizeRoi.x * scaleRatio);
    scaledRoi.y = cvRound(fullSizeRoi.y * scaleRatio);
    scaledRoi.width = cvRound(fullSizeRoi.width * scaleRatio);
    scaledRoi.height = cvRound(fullSizeRoi.height * scaleRatio);
}

void ScalableRoi::setFullSize(int x, int y, int width, int height, double scaleRatio, const ScalableRoi& container) {
    fullSizeRoi.x = std::clamp(x, container.getFullSize().x, container.getFullSize().x + container.getFullSize().width);
    fullSizeRoi.y = std::clamp(y, container.getFullSize().y, container.getFullSize().y + container.getFullSize().height);
    fullSizeRoi.width = std::clamp(width, 0, container.getFullSize().x + container.getFullSize().width - fullSizeRoi.x);
    fullSizeRoi.height = std::clamp(height, 0, container.getFullSize().y + container.getFullSize().height - fullSizeRoi.y);

    scaledRoi.x = std::clamp(
            cvRound(x * scaleRatio),
            container.getScaled().x,
            container.getScaled().x + container.getScaled().width);
    scaledRoi.y = std::clamp(
            cvRound(y * scaleRatio),
            container.getScaled().y,
            container.getScaled().y + container.getScaled().height);
    scaledRoi.width = std::clamp(
            cvRound(width * scaleRatio),
            0,
            container.getScaled().x + container.getScaled().width - scaledRoi.x);
    scaledRoi.height = std::clamp(
            cvRound(height * scaleRatio),
            0,
            container.getScaled().y + container.getScaled().height - scaledRoi.y);
}

void ScalableRoi::setScaled(int scaledX, int scaledY, int scaledWidth, int scaledHeight, double scaleRatio) {
    scaledRoi.x = std::max(scaledX, 0);
    scaledRoi.y = std::max(scaledY, 0);
    scaledRoi.width = std::max(scaledWidth, 0);
    scaledRoi.height = std::max(scaledHeight, 0);

    fullSizeRoi.x = cvRound(scaledRoi.x / scaleRatio);
    fullSizeRoi.y = cvRound(scaledRoi.y / scaleRatio);
    fullSizeRoi.width = cvRound(scaledRoi.width / scaleRatio);
    fullSizeRoi.height = cvRound(scaledRoi.height / scaleRatio);
}

void ScalableRoi::clear() {
    fullSizeRoi.x = 0;
    fullSizeRoi.y = 0;
    fullSizeRoi.width = 0;
    fullSizeRoi.height = 0;

    scaledRoi.x = 0;
    scaledRoi.y = 0;
    scaledRoi.width = 0;
    scaledRoi.height = 0;
}

cv::Rect ScalableRoi::getFullSize() const {
    return fullSizeRoi;
}

cv::Rect ScalableRoi::getScaled() const {
    return scaledRoi;
}

bool ScalableRoi::containsOrEquals(const ScalableRoi& other) const{
    return isRoiContainsOrEquals(fullSizeRoi, other.fullSizeRoi) && isRoiContainsOrEquals(scaledRoi, other.scaledRoi);
}

bool ScalableRoi::isBiggerOrEquals(const ScalableRoi& other) const {
    return isRoiBiggerOrEquals(fullSizeRoi, other.fullSizeRoi) && isRoiBiggerOrEquals(scaledRoi, other.scaledRoi);;
}

bool ScalableRoi::isRoiContainsOrEquals(const cv::Rect &roi, const cv::Rect &other) {
    return roi.x <= other.x && roi.y <= other.y && isRoiBiggerOrEquals(roi, other);
}

bool ScalableRoi::isRoiBiggerOrEquals(const cv::Rect &roi, const cv::Rect &other) {
    return roi.width >= other.width && roi.height >= other.height;
}
