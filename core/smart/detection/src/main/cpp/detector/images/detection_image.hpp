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

#ifndef KLICK_R_DETECTION_IMAGE_HPP
#define KLICK_R_DETECTION_IMAGE_HPP

#include <opencv2/core/types.hpp>

namespace smartautoclicker {

    class DetectionImage {

    protected:
        std::unique_ptr<cv::Mat> colorMat = std::make_unique<cv::Mat>();
        std::unique_ptr<cv::Mat> grayMat = std::make_unique<cv::Mat>();

    public:
        [[nodiscard]] const cv::Mat* getColorMat() const;
        [[nodiscard]] const cv::Mat* getGrayMat() const;
        [[nodiscard]] cv::Rect getRoi() const;
    };
}

#endif //KLICK_R_DETECTION_IMAGE_HPP
