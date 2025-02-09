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

#include "scalable_roi.hpp"

using namespace smartautoclicker;


void ScalableRoi::setFullSize(const cv::Rect& fullSizeRoi, double scaleRatio) {
    setFullSize(fullSizeRoi.x, fullSizeRoi.y, fullSizeRoi.width, fullSizeRoi.height, scaleRatio);
}

void ScalableRoi::setFullSize(int x, int y, int width, int height, double scaleRatio) {
    fullSize.x = x;
    fullSize.y = y;
    fullSize.width = width;
    fullSize.height = height;

    scaled.x = cvRound(x * scaleRatio);
    scaled.y = cvRound(y * scaleRatio);
    scaled.width = cvRound(width * scaleRatio);
    scaled.height = cvRound(height * scaleRatio);
}

void ScalableRoi::setScaled(int x, int y, int width, int height, double scaleRatio) {
    scaled.x = x;
    scaled.y = y;
    scaled.width = width;
    scaled.height = height;

    fullSize.x = cvRound(x / scaleRatio);
    fullSize.y = cvRound(y / scaleRatio);
    fullSize.width = cvRound(width / scaleRatio);
    fullSize.height = cvRound(height / scaleRatio);
}

void ScalableRoi::clear() {
    fullSize.x = 0;
    fullSize.y = 0;
    fullSize.width = 0;
    fullSize.height = 0;

    scaled.x = 0;
    scaled.y = 0;
    scaled.width = 0;
    scaled.height = 0;
}

int ScalableRoi::fullSizeCenterX() const {
    return fullSize.x + ((int) (fullSize.width / 2));
}

int ScalableRoi::fullSizeCenterY() const {
    return fullSize.y + ((int) (fullSize.height / 2));
}

bool ScalableRoi::isEmpty() const {
    return fullSize.width == 0 || fullSize.height == 0 || scaled.width == 0 || scaled.height == 0;
}


