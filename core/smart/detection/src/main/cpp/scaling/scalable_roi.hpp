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
    private:
        cv::Rect fullSizeRoi;
        cv::Rect scaledRoi;

        static bool isRoiContainsOrEquals(const cv::Rect& roi, const cv::Rect& other);
        static bool isRoiBiggerOrEquals(const cv::Rect& roi, const cv::Rect& other);

    public:
        cv::Rect getFullSize() const;
        cv::Rect getScaled() const;

        bool containsOrEquals(const ScalableRoi& roi) const;
        bool isBiggerOrEquals(const ScalableRoi& other) const;

        void setFullSize(cv::Mat* fullSize, double scaleRatio);
        void setFullSize(int x, int y, int width, int height, double scaleRatio, const ScalableRoi& container);
        void setScaled(int scaledX, int scaledY, int scaledWidth, int scaledHeight, double scaleRatio, const ScalableRoi& container);
        void clear();
    };

} // smartautoclicker

#endif //KLICK_R_SCALABLE_ROI_HPP
