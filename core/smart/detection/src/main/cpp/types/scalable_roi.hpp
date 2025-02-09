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

#ifndef KLICK_R_SCALABLE_ROI_HPP
#define KLICK_R_SCALABLE_ROI_HPP

#include <opencv2/core/types.hpp>

namespace smartautoclicker {

    class ScalableRoi {
    public:
        cv::Rect fullSize;
        cv::Rect scaled;

        void setFullSize(const cv::Rect& fullSizeRoi, double scaleRatio);
        void setFullSize(int x, int y, int width, int height, double scaleRatio);
        void setScaled(int x, int y, int width, int height, double scaleRatio);
        void clear();

        int fullSizeCenterX() const;
        int fullSizeCenterY() const;
        bool isEmpty() const;
    };
}

#endif //KLICK_R_SCALABLE_ROI_HPP
