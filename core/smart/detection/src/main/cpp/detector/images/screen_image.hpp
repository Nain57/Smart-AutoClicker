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


#ifndef KLICK_R_SCREEN_IMAGE_HPP
#define KLICK_R_SCREEN_IMAGE_HPP

#include "detection_image.hpp"

namespace smartautoclicker {

    class ScreenImage : public DetectionImage {

    private:
        static cv::Mat cropMat(const cv::Mat& mat, const cv::Rect& roi);

    public:
        void processNewData(std::unique_ptr<cv::Mat> newData, const char* metricsTag);

        [[nodiscard]] cv::Mat cropColor(const cv::Rect& roi) const;
        [[nodiscard]] cv::Mat cropGray(const cv::Rect& roi) const;
    };
}

#endif //KLICK_R_SCREEN_IMAGE_HPP
