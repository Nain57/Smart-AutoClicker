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

using namespace smartautoclicker;

void ScalableRoi::setFullSize(cv::Mat* fullSize, double scaleRatio) {
    setFullSize(cv::Rect(0, 0, fullSize->cols, fullSize->rows), scaleRatio);
}

void ScalableRoi::setFullSize(cv::Rect fullSize, double scaleRatio) {
    fullSizeRoi.x = fullSize.x;
    fullSizeRoi.y = fullSize.y;
    fullSizeRoi.width = fullSize.width;
    fullSizeRoi.height = fullSize.height;

    scaledRoi.x = cvRound(fullSize.x * scaleRatio);
    scaledRoi.y = cvRound(fullSize.y * scaleRatio);
    scaledRoi.width = std::max(1, cvRound(fullSize.width * scaleRatio));
    scaledRoi.height = std::max(1, cvRound(fullSize.height * scaleRatio));
}

void ScalableRoi::setScaled(int scaledX, int scaledY, int scaledWidth, int scaledHeight, double scaleRatio) {
    fullSizeRoi.x = cvRound(scaledX / scaleRatio);
    fullSizeRoi.y = cvRound(scaledY / scaleRatio);
    fullSizeRoi.width = cvRound(scaledWidth / scaleRatio);
    fullSizeRoi.height = cvRound(scaledHeight / scaleRatio);

    scaledRoi.x = scaledX;
    scaledRoi.y = scaledY;
    scaledRoi.width = scaledWidth;
    scaledRoi.height = scaledHeight;
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
